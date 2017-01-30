package org.transdroid.connect.util;

import org.reactivestreams.Publisher;

import java.util.Arrays;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.functions.Function;

public final class RxUtil {

	private RxUtil() {}

	public static <T> FlowableTransformer<T[], T> asList() {
		return new FlowableTransformer<T[], T>() {
			@Override
			public Publisher<T> apply(Flowable<T[]> upstream) {
				return upstream.flatMapIterable(new Function<T[], Iterable<T>>() {
					@Override
					public Iterable<T> apply(T[] ts) throws Exception {
						return Arrays.asList(ts);
					}
				});
			}
		};
	}

}
