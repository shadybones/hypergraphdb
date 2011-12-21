/* 
 * This file is part of the HyperGraphDB source distribution. This is copyrighted 
 * software. For permitted uses, licensing options and redistribution, please see  
 * the LicensingInformation file at the root level of the distribution.  
 * 
 * Copyright (c) 2005-2010 Kobrix Software, Inc.  All rights reserved. 
 */
package org.hypergraphdb.cache;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hypergraphdb.HGAtomCache;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.IncidenceSet;
import org.hypergraphdb.handle.DefaultManagedLiveHandle;
import org.hypergraphdb.handle.HGLiveHandle;
import org.hypergraphdb.handle.HGManagedLiveHandle;
import org.hypergraphdb.handle.WeakHandle;
import org.hypergraphdb.handle.WeakManagedHandle;
import org.hypergraphdb.util.CloseMe;
import org.hypergraphdb.util.WeakIdentityHashMap;

/**
 * 
 * <p>
 * This cache implementation interacts with the Java garbage collector, by using
 * the <code>java.lang.ref</code> facilities, in order to implement its eviction 
 * policy. The eviction policy is the following: an atom is removed from the cache
 * if and only if either (1) its runtime Java instance is garbage collected or (2)
 * it is explicitly removed from the HyperGraph DB. Freezing an atom will keep it in
 * the cache even when it's garbage collected, but not if it's removed from DB altogether.  
 * </p>
 * 
 * <p>
 * The <code>WeakRefAtomCache</code> is a bit misleading: weak reference are in fact
 * not used at all. Soft references are used when caching incidence sets and phantom
 * references are used for atoms (@see org.hypergraphdb.handle.PhantomHandle).
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class WeakRefAtomCache implements HGAtomCache 
{	
	private HyperGraph graph = null;
	
	private HGCache<HGPersistentHandle, IncidenceSet> incidenceCache = null; // to be configured by the HyperGraph instance
	
    private final Map<HGPersistentHandle, WeakHandle> 
    	liveHandles = new HashMap<HGPersistentHandle, WeakHandle>();
		
	private Map<Object, HGLiveHandle> atoms = 
		new WeakIdentityHashMap<Object, HGLiveHandle>();
	
	private Map<HGLiveHandle, Object> frozenAtoms =	new IdentityHashMap<HGLiveHandle, Object>();
	
	private ColdAtoms coldAtoms = new ColdAtoms();
	
	public static final long DEFAULT_PHANTOM_QUEUE_POLL_INTERVAL = 500;
	
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>();
	private PhantomCleanup cleanupThread = new PhantomCleanup();
	private long phantomQueuePollInterval = DEFAULT_PHANTOM_QUEUE_POLL_INTERVAL;
	private boolean closing = false;
	
	//
	// This handle class is used to read atoms during closing of the cache. Because
	// closing the HyperGraph may involve a lot of cleanup activity where it's necessary
	// to read atoms (mostly types) into main memory just temporarily, we need some
	// way to enact this temporarity. 
	//
	private static class TempLiveHandle extends DefaultManagedLiveHandle
	{
		public TempLiveHandle(Object ref, HGPersistentHandle persistentHandle, byte flags)
		{
			super(ref, persistentHandle, flags, 0, 0);
		}
		
		public void setRef(Object ref)
		{
			this.ref = ref;
		}
	}	
	
	private void processRefQueue() throws InterruptedException
	{
		WeakHandle ref = (WeakHandle)refQueue.remove(phantomQueuePollInterval);
		while (ref != null)
		{
//			graph.getEventManager().dispatch(graph, 
//					 new HGAtomEvictEvent(ref, ref.fetchRef()));
			lock.writeLock().lock();
			try
			{
				liveHandles.remove(ref.getPersistentHandle());
			}
			finally
			{
				lock.writeLock().unlock();
			}
			ref.clear();
			synchronized (ref) { ref.notifyAll(); }			
			ref = (WeakHandle)refQueue.poll();
		}
	}
	
	private class PhantomCleanup extends Thread 
	{
		private boolean done;
		
	    public void run() 
	    {
//			WeakHandle.returnEnqueued.set(Boolean.TRUE);
	        for (done = false; !done; ) 
	        {
	        	try 
	            {
	        		processRefQueue();
	            } 
	        	catch (InterruptedException exc) 
	        	{
	                Thread.currentThread().interrupt();
	            }
	        	catch (Throwable t)
	        	{
	        		System.err.println("PhantomCleanup thread caught an unexpected exception, stack trace follows:");
	        		t.printStackTrace(System.err);
	        	}
	        }
//			WeakHandle.returnEnqueued.set(Boolean.FALSE);
	    }

	    public void end()
	    {
	    	this.done = true;
	    }
	}
	
	public WeakRefAtomCache()
	{
		cleanupThread.setPriority(Thread.MAX_PRIORITY);
		cleanupThread.setDaemon(true);		
		cleanupThread.start();
	}
	
	public void setIncidenceCache(HGCache<HGPersistentHandle, IncidenceSet> cache)
	{
		this.incidenceCache= cache;
	}
	
	public HGCache<HGPersistentHandle, IncidenceSet> getIncidenceCache()
	{
		return incidenceCache;
	}
	
	public void setHyperGraph(HyperGraph hg) 
	{
		this.graph = hg;
		cleanupThread.setName("HGCACHE Cleanup - " + graph.getLocation());
	}
	
	public HGLiveHandle atomRead(HGPersistentHandle pHandle, 
								 Object atom,
								 byte flags) 
	{
		if (closing)
		{
			HGLiveHandle result = new TempLiveHandle(atom, pHandle, flags);
			atoms.put(atom, result);
			return result;
		}		
		lock.writeLock().lock();
		WeakHandle h = null;
		try
		{			
			h = liveHandles.get(pHandle);
			if (h != null)
				return h;
			h = new WeakHandle(atom, pHandle, flags, refQueue);			
			atoms.put(atom, h);
			liveHandles.put(pHandle, h);
			coldAtoms.add(atom);
		}
		finally
		{
			lock.writeLock().unlock();
		}
		return h;
	}

	public HGManagedLiveHandle atomRead(HGPersistentHandle pHandle,
									    Object atom, 
									    byte flags, 
									    long retrievalCount, 
									    long lastAccessTime) 
	{
		if (closing)
		{
			HGManagedLiveHandle result = new TempLiveHandle(atom, pHandle, flags);
			atoms.put(atom, result);
			return result;
		}
		WeakManagedHandle h = null;
		lock.writeLock().lock();
		try
		{
			h = (WeakManagedHandle)liveHandles.get(pHandle);
			if (h != null)
				return h;
			h = new WeakManagedHandle(atom, 
										 pHandle, 
										 flags, 
										 refQueue,
										 retrievalCount,
										 lastAccessTime);
			atoms.put(atom, h);
			liveHandles.put(pHandle, h);
			coldAtoms.add(atom);
		}
		finally
		{
			lock.writeLock().unlock();
		}
		return h;
	}

	public void atomRefresh(HGLiveHandle handle, Object atom) 
	{
		if (closing)
		{
			if (handle instanceof WeakHandle)
				((WeakHandle)handle).clear();
			else
				((TempLiveHandle)handle).setRef(atom);
			return;
		}
		if (handle == null)
			throw new NullPointerException("atomRefresh: handle is null.");
		
		lock.writeLock().lock();
		
		try
		{
		    WeakHandle newLive = null;
		    if (handle instanceof WeakManagedHandle)
		        newLive = new WeakManagedHandle(atom, 
		                                        handle.getPersistentHandle(), 
		                                        handle.getFlags(), 
		                                        refQueue,
		                                        ((WeakManagedHandle)handle).getRetrievalCount(),
		                                        ((WeakManagedHandle)handle).getRetrievalCount());
		    else
		        newLive = new WeakHandle(atom, 
		                                 handle.getPersistentHandle(),
		                                 handle.getFlags(),
		                                 refQueue);
		    liveHandles.put(handle.getPersistentHandle(), newLive);
		    Object curr = handle.getRef();
		    if (curr != null)
		    {
		        atoms.remove(curr);
		    }
		    atoms.put(atom, newLive);
            coldAtoms.add(atom);
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}

	public void close() 
	{	
		closing = true;
		cleanupThread.end();	
		while (cleanupThread.isAlive() )
			try { cleanupThread.join(); } catch (InterruptedException ex) { }
//		WeakHandle.returnEnqueued.set(Boolean.TRUE);
//		try { processRefQueue(); } catch (InterruptedException ex) { }
//		for (Iterator<Map.Entry<HGPersistentHandle, WeakHandle>> i = liveHandles.entrySet().iterator(); 
//			 i.hasNext(); ) 
//		{
//		    WeakHandle h = i.next().getValue();
//			Object x = h.fetchRef();
//			graph.getEventManager().dispatch(graph, new HGAtomEvictEvent(h, x));			
//			if (h.isEnqueued())
//			{
//				h.clear();
//			}
//		}
//		WeakHandle.returnEnqueued.set(Boolean.FALSE);
		frozenAtoms.clear();		
		incidenceCache.clear();
		if (incidenceCache instanceof CloseMe)
			((CloseMe)incidenceCache).close();
		atoms.clear();
		liveHandles.clear();
	}

	public HGLiveHandle get(HGPersistentHandle pHandle) 
	{
		lock.readLock().lock();
		try
		{
		    WeakHandle h = liveHandles.get(pHandle);
			if (h != null)
				h.accessed();
			return h;
		}
		finally
		{
			lock.readLock().unlock();			
		}
	}

	public HGLiveHandle get(Object atom) 
	{
		lock.readLock().lock();
		try
		{
			return atoms.get(atom);
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	public void remove(HGLiveHandle handle) 
	{
		lock.writeLock().lock();
		try
		{
			atoms.remove(handle.getRef());
			// Shouldn't use clear here, since we might be gc-ing the ref!
//			if (handle instanceof WeakHandle)
//				((WeakHandle)handle).storeRef(null);
			liveHandles.remove(handle.getPersistentHandle());			
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}

	
	public boolean isFrozen(HGLiveHandle handle) 
	{
		synchronized (frozenAtoms)
		{
			return frozenAtoms.containsKey(handle);
		}
	}

	public void freeze(HGLiveHandle handle) 
	{
		Object atom = handle.getRef();
		if (atom != null)
			synchronized (frozenAtoms)
			{
				frozenAtoms.put(handle, atom);
			}
	}	

	public void unfreeze(HGLiveHandle handle) 
	{
		synchronized (frozenAtoms)
		{		
			frozenAtoms.remove(handle);
		}
	}
}