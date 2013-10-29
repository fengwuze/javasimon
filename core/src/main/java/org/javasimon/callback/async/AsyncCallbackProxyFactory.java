package org.javasimon.callback.async;

import java.lang.reflect.Method;

import org.javasimon.SimonManager;
import org.javasimon.callback.Callback;
import org.javasimon.proxy.Delegating;
import org.javasimon.proxy.DelegatingMethodInvocation;

/**
 * Callback factory, produces a callback wrapper to make any callback asynchronous.
 * <p/>
 * Example: {@code Callback myAsyncCallback=new AsyncCallbackProxy(myCallback).newProxy(); }
 * <p/>
 * The purpose of this wrapping callback is to prevent the wrapped callback
 * from being executed on the main thread. This can be useful when a concrete
 * callback is time consuming, to reduce the impact on application performances.
 * <p/>
 * It can be used to disable/enable, at runtime, a callback without removing it from
 * the {@link SimonManager}: {@code asyncCallbackProxy.setExecutor(Executors.disabled);}
 *
 * @author gerald
 */
public final class AsyncCallbackProxyFactory extends ExecutorProxyFactory<Callback> {
	/**
	 * Interfaces implemented by callback proxy
	 */
	private static final Class[] PROXY_INTERFACES = new Class[] {Callback.class, Delegating.class};
	private final Method getDelegateMethod;

	/**
	 * Constructor
	 *
	 * @param delegate Wrapped object
	 */
	public AsyncCallbackProxyFactory(Callback delegate) {
		super(delegate);
		getDelegateMethod = findGetDelegateMethod();
	}

	/**
	 * Constructor.
	 *
	 * @param delegate Wrapped object
	 * @param executor Executor used to run callback method, see {@link Executors}
	 */
	public AsyncCallbackProxyFactory(Callback delegate, Executor executor) {
		super(delegate, executor);
		getDelegateMethod = findGetDelegateMethod();
	}

	/**
	 * Find {@link Delegating#getDelegate() } method
	 *
	 * @return Get delegate method
	 */
	private Method findGetDelegateMethod() {
		try {
			return Delegating.class.getDeclaredMethod("getDelegate", new Class[0]);
		} catch (NoSuchMethodException noSuchMethodException) {
			throw new IllegalStateException("getDelegate method not found on Delegating interface", noSuchMethodException);
		} catch (SecurityException securityException) {
			throw new IllegalStateException("getDelegate method not found on Delegating interface", securityException);
		}
	}

	/**
	 * Creates a callback proxy.
	 *
	 * @param classLoader Class loader
	 * @return Callback proxy.
	 */
	public Callback newProxy(ClassLoader classLoader) {
		return (Callback) newProxy(classLoader, PROXY_INTERFACES);
	}

	/**
	 * Creates a callback proxy.
	 * The class loader for current thread is used as default class loader.
	 *
	 * @return Callback proxy.
	 */
	public Callback newProxy() {
		return (Callback) newProxy(PROXY_INTERFACES);
	}

	@Override
	protected Object invoke(DelegatingMethodInvocation<Callback> delegatingMethodInvocation) throws Throwable {
		Object result;
		if (delegatingMethodInvocation.getMethod().equals(getDelegateMethod)) {
			result = getDelegate();
		} else {
			result = super.invoke(delegatingMethodInvocation);
		}
		return result;
	}
}
