package modtools.utils;

import arc.func.*;
import modtools.events.ExecuteTree.OK;
import modtools.utils.Tools.CBoolp;

import java.util.function.*;

public class SR<T> {
	T value;

	public SR(T value) {
		this.value = value;
	}

	public SR<T> setv(T value) {
		this.value = value;
		return this;
	}
	public SR<T> setOpt(Function<T, T> func) {
		if (func != null) value = func.apply(value);
		return this;
	}
	public SR<T> setnull(Predicate<T> condition) {
		return set(condition, (T) null);
	}

	public SR<T> set(Predicate<T> conditon, T newValue) {
		if (conditon != null && conditon.test(value)) value = newValue;
		return this;
	}
	public SR<T> set(Predicate<T> conditon, Supplier<T> newValue) {
		if (conditon != null && conditon.test(value)) value = newValue.get();
		return this;
	}
	public <R> SR<R> reset(Function<T, R> func) {
		return new SR<>(func.apply(value));
	}

	/**
	 * {@link SR#value}是否存在（不为{@code null}）
	 * 存在就执行代码
	 */
	public SR<T> ifPresent(Consumer<T> cons) {
		return ifPresent(cons, null);
	}

	public SR<T> ifPresent(Consumer<T> cons, Runnable nullrun) {
		if (value != null) cons.accept(value);
		else if (nullrun != null) nullrun.run();
		return this;
	}

	/**
	 * @param cons 如果满足就执行
	 * @throws RuntimeException 当执行后抛出
	 */
	public <R> SR<T> isInstance(Class<R> cls, Consumer<R> cons) throws SatisfyException {
		if (cls.isInstance(value)) {
			cons.accept(cls.cast(value));
			throw new SatisfyException();
		}
		return this;
	}

	public SR<T> cons(boolean b, TBoolc<T> cons) {
		cons.get(value, b);
		return this;
	}
	public SR<T> cons(float f, TFloatc<T> cons) {
		cons.get(value, f);
		return this;
	}
	public SR<T> cons(int i, TIntc<T> cons) {
		cons.get(value, i);
		return this;
	}
	public <R> SR<T> cons(R obj, BiConsumer<T, R> cons) {
		cons.accept(value, obj);
		return this;
	}

	public SR<T> cons(Consumer<T> cons) {
		cons.accept(value);
		return this;
	}
	public Runnable asRun(Consumer<T> cons) {
		return () -> cons.accept(value);
	}
	public SR<T> cons(Predicate<T> boolf, Consumer<T> cons) {
		if (boolf.test(value)) cons.accept(value);
		return this;
	}
	public SR<T> consNot(Predicate<T> boolf, Consumer<T> cons) {
		if (!boolf.test(value)) cons.accept(value);
		return this;
	}


	public SR<T> ifRun(boolean b, Consumer<T> cons) {
		if (b) cons.accept(value);
		return this;
	}

	/** @return {@code true} if valid. */
	public boolean test(Predicate<T> predicate) {
		return value != null && predicate.test(value);
	}
	public T get() {
		return value;
	}
	public <R> R get(Function<T, R> func) {
		return func.apply(value);
	}
	public <R> R catchGet(FunctionCatch<T, R> func, Function<T, R> catchF) {
		try {
			return func.apply(value);
		} catch (Throwable e) {
			return catchF.apply(value);
		}
	}

	/* ---- for classes ---- */
	public static final SR NONE = new SR<>(null) {
		public SR<Object> isExtend(Consumer<Class<?>> cons, Class<?>... classes) {
			return this;
		}
	};
	/**
	 * 判断是否继承
	 * @param cons    形参为满足的{@code class}
	 * @param classes 判断的类
	 */
	public SR<T> isExtend(Consumer<Class<?>> cons, Class<?>... classes) {
		if (!(value instanceof Class<?> origin)) throw new IllegalStateException("Value isn't a class");

		for (Class<?> cl : classes) {
			if (cl.isAssignableFrom(origin)) {
				cons.accept(cl);
				return NONE;
			}
		}
		return this;
	}

	public static void catchSatisfy(Runnable run) {
		try {
			run.run();
		} catch (SatisfyException ignored) {}
	}

	public static class SatisfyException extends RuntimeException {}
	public interface TBoolc<T> {
		void get(T p1, boolean p2);
	}
	public interface TFloatc<T> {
		void get(T p1, float p2);
	}
	public interface TIntc<T> {
		void get(T p1, int p2);
	}

	public static <P1, V> Cons<V> makeCons(P1 p1, Cons2<V, P1> cons2) {
		return v -> cons2.get(v, p1);
	}

	/**
	 * 使用方法:<br />
	 * + {@link CatchSR#apply(Runnable run)}<br />
	 * run是get链<br />
	 *
	 * <pre>{@code CatchSR.apply(() -> CatchSR.of(
	 * () -> MyReflect.lookupGetMethods(cls))
	 * .get(cls::getDeclaredMethods)
	 * .get(() -> new Method[0])}</pre>
	 */
	public static class CatchSR<R> {
		private              R       value;
		private static final CatchSR instance = new CatchSR();
		private CatchSR() {}
		public static <R> CatchSR<R> of(Prov<R> prov) {
			instance.value = null;
			return instance.get(prov);
		}
		public static <R> R apply(Runnable run) {
			try {
				run.run();
				throw new IllegalStateException("Cannot meet the requirements.");
			} catch (SatisfyException e) {
				return (R) instance.value;
			}
		}
		public CatchSR<R> get(Prov<R> prov) {
			try {
				value = prov.get();
			} catch (Throwable ignored) {
				return this;
			}
			throw new SatisfyException();
		}
		public R get() {
			return value;
		}
	}
	public static boolean getIgnoreException(CBoolp run, boolean def) {
		try {
			return run.get();
		} catch (Throwable ignored) {
			return def;
		}
	}
	public interface FunctionCatch<P, R> {
		R apply(P p) throws Throwable;
	}

	public static class MatchSR<T, R> extends SR<T> {
		public MatchSR(T value) {
			super(value);
		}
		R matchValue;
		/**
		 * @param cons 如果满足就执行
		 * @throws RuntimeException 当执行后抛出
		 */
		public <P extends T> MatchSR<T, R> match(Class<P> cls, Function<P, R> cons) throws SatisfyException {
			if (matchValue == null && cls.isInstance(value)) {
				matchValue = cons.apply(cls.cast(value));
			}
			return this;
		}
		public R matchValue() {
			return matchValue;
		}
	}
}
