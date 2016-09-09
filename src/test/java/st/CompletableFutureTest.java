package st;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Test;

public class CompletableFutureTest {

	Supplier<Integer> task1 = () -> {
		System.out.println("Task 1 begin " + threadId());
		sleepSafe(2000);
		System.out.println("Task 1 done");
		return 42;
	};

	Function<Integer, String> task2 = input -> {
		System.out.println("Task 2 begin " + threadId());
		sleepSafe(1000);
		System.out.println("Task 2 done");
		return "!" + input;
	};

	Runnable task3 = () -> {
		System.out.println("Task 3 begin " + threadId());
		sleepSafe(3000);
		System.out.println("Task 3 done");
	};

	Runnable task4 = () -> {
		System.out.println("Task 4 begin " + threadId());
		sleepSafe(2000);
		System.out.println("Task 4 done");
	};

	Supplier<String> task5 = () -> {
		System.out.println("Task 5 begin " + threadId());
		sleepSafe(1000);
		System.out.println("Task 5 done");
		return "666";
	};

	CompletableFuture<Integer> doTask1() {
		return CompletableFuture.supplyAsync(task1);
	}

	CompletableFuture<String> doTask2(Integer input) {
		return CompletableFuture.supplyAsync(() -> task2.apply(input));
	}

	CompletableFuture<Void> doTask3() {
		return CompletableFuture.runAsync(task3);
	}

	CompletableFuture<Void> doTask4() {
		return CompletableFuture.runAsync(task4);
	}

	CompletableFuture<String> doTask5() {
		return CompletableFuture.supplyAsync(task5);
	}

	<T> CompletableFuture<T> timeoutAfter(Duration duration) {
		CompletableFuture<T> future = new CompletableFuture<>();
		Executors.newSingleThreadScheduledExecutor()
				.schedule(() -> future.completeExceptionally(new TimeoutException()), duration.toMillis(), TimeUnit.MILLISECONDS);
		return future;
	}

	<T> CompletableFuture<T> timeoutAfter(Duration duration, T defaultValue) {
		CompletableFuture<T> future = new CompletableFuture<>();
		Executors.newSingleThreadScheduledExecutor()
				.schedule(() -> future.complete(defaultValue), duration.toMillis(), TimeUnit.MILLISECONDS);
		return future;
	}


	@Test
	public void testChaining() throws Exception {
		CompletableFuture<String> t12 = doTask1()
				.thenComposeAsync(this::doTask2);
		CompletableFuture<Void> t3 = doTask3();
		CompletableFuture<String> t45 = doTask4()
				.thenCompose(v -> doTask5());

		CompletableFuture.allOf(t12, t3, t45)
				.thenAccept(v -> System.out.println("ALL DONE. Got results: " + t12.join() + ", " + t45.join()))
				.exceptionally(exception -> {
					System.out.println(exception.toString());
					return null;
				});

		sleepSafe(5000);
	}

	@Test
	public void timeoutTest() throws Exception {
		CompletableFuture<Object> any = CompletableFuture.anyOf(timeoutAfter(Duration.ofSeconds(4)), doTask3());
		any.thenAccept(v -> System.out.println("DONE"))
				.exceptionally(exception -> {
					System.out.println("Exception");
					return null;
				});

		sleepSafe(5000);
	}

	@Test
	public void defaultValueTest() throws Exception {
		CompletableFuture<String> t5 = doTask5();
		CompletableFuture<Object> any = CompletableFuture.anyOf(timeoutAfter(Duration.ofSeconds(4), "<Unknown>"), t5);
		any.thenAccept(v -> System.out.println(any.join()));

		sleepSafe(5000);

	}

	private void sleepSafe(long duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
		}
	}

	private String threadId() {
		return Thread.currentThread()
				.getName();
	}
}
