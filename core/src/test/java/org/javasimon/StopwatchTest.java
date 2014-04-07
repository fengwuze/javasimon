package org.javasimon;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link Stopwatch}.
 *
 * @author <a href="mailto:virgo47@gmail.com">Richard "Virgo" Richter</a>
 */
public final class StopwatchTest extends SimonUnitTest {

	@Test
	public void basicStopwatchTest() {
		Stopwatch stopwatch = SimonManager.getStopwatch(null);
		long split = stopwatch.start().stop().runningFor();
		Assert.assertTrue(stopwatch.getTotal() >= 0);
		Assert.assertEquals(stopwatch.getTotal(), split);
		Assert.assertEquals(stopwatch.getCounter(), 1);
		Assert.assertEquals(stopwatch.getMax(), stopwatch.getTotal());
		Assert.assertEquals(stopwatch.getMin(), stopwatch.getTotal());
		assertStopwatchAndSampleAreEqual(stopwatch);
	}

	@Test
	public void usagesTest() throws Exception {
		TestClock clock = new TestClock();
		clock.setMillisNanosFollow(10);
		EnabledManager manager = new EnabledManager(clock);

		Stopwatch stopwatch = manager.getStopwatch(null);
		Assert.assertEquals(stopwatch.getFirstUsage(), 0);
		Assert.assertEquals(stopwatch.getLastUsage(), 0);
		Split split = stopwatch.start();
		Assert.assertEquals(stopwatch.getFirstUsage(), stopwatch.getLastUsage());
		split.stop();
		assertStopwatchAndSampleAreEqual(stopwatch);
		Assert.assertTrue(stopwatch.getFirstUsage() <= stopwatch.getLastUsage());

		clock.setMillisNanosFollow(30);

		stopwatch.addSplit(Split.create(0));
		Assert.assertTrue(stopwatch.getFirstUsage() < stopwatch.getLastUsage());
		assertStopwatchAndSampleAreEqual(stopwatch);
	}

	// remove in 4.0
	@Test
	@Deprecated
	public void resetTest() throws Exception {
		// with raw current millis this test is unstable - this is not a problem in real-life situations though
		// point is to check that timestamps are set, not that they are set off by 1 ms or so
		TestClock testClock = new TestClock();
		testClock.setMillisNanosFollow(1000);
		long ts = testClock.milliTime();
		Stopwatch stopwatch = SimonManager.getStopwatch(null);
		stopwatch.reset();
		stopwatch.addSplit(Split.create(100));
		Assert.assertEquals(stopwatch.getTotal(), 100);
		Assert.assertEquals(stopwatch.getMax(), 100);
		Assert.assertEquals(stopwatch.getMin(), 100);
		long maxTimestamp = stopwatch.getMaxTimestamp();
		Assert.assertTrue(maxTimestamp >= ts, "maxTimestamp=" + maxTimestamp + ", ts=" + ts);
		Assert.assertEquals(stopwatch.getMinTimestamp(), maxTimestamp);
		Assert.assertEquals(stopwatch.getLastUsage(), maxTimestamp);
		Assert.assertEquals(stopwatch.getFirstUsage(), maxTimestamp);
		Assert.assertEquals(stopwatch.getCounter(), 1);
		StopwatchSample sample = stopwatch.sample();
		Assert.assertEquals(sample.getName(), stopwatch.getName());
		Assert.assertEquals(sample.getCounter(), 1);
		Assert.assertEquals(sample.getTotal(), 100);
		Assert.assertEquals(sample.getMean(), 100d);
		Assert.assertEquals(sample.getStandardDeviation(), 0d);
		Assert.assertEquals(sample.getVariance(), 0d);
		Assert.assertEquals(sample.getVarianceN(), 0d);

		stopwatch.reset();
		Assert.assertEquals(stopwatch.getTotal(), 0);
		Assert.assertEquals(stopwatch.getMax(), 0);
		Assert.assertEquals(stopwatch.getMin(), Long.MAX_VALUE);
		Assert.assertEquals(stopwatch.getMaxTimestamp(), 0);
		Assert.assertEquals(stopwatch.getMinTimestamp(), 0);
		Assert.assertTrue(stopwatch.getLastUsage() >= ts); // usages are NOT clear!
		Assert.assertTrue(stopwatch.getFirstUsage() >= ts);
		Assert.assertEquals(stopwatch.getCounter(), 0);
		sample = stopwatch.sample();
		Assert.assertEquals(sample.getCounter(), 0);
		Assert.assertEquals(sample.getTotal(), 0);
		Assert.assertEquals(sample.getMean(), 0d);
		Assert.assertEquals(sample.getStandardDeviation(), Double.NaN);
		Assert.assertEquals(sample.getVariance(), Double.NaN);
		Assert.assertEquals(sample.getVarianceN(), Double.NaN);
	}

	@Test
	public void disableEnableInsideSplit() throws Exception {
		Stopwatch stopwatch = SimonManager.getStopwatch(null);
		Split split = stopwatch.start();
		Assert.assertEquals(stopwatch.getActive(), 1);
		stopwatch.setState(SimonState.DISABLED, false);
		Assert.assertEquals(stopwatch.getActive(), 1);
		split.stop();
		Assert.assertEquals(stopwatch.getActive(), 0);
		Assert.assertTrue(stopwatch.getTotal() > 0);
		Assert.assertEquals(stopwatch.getCounter(), 1);
		assertStopwatchAndSampleAreEqual(stopwatch);

		// split started on enabled stopwatch does have an effect
		long total = stopwatch.getTotal();
		long counter = stopwatch.getCounter();

		split = stopwatch.start();
		Assert.assertEquals(stopwatch.getActive(), 0);
		stopwatch.setState(SimonState.ENABLED, false);
		Assert.assertEquals(stopwatch.getActive(), 0);
		split.stop();
		Assert.assertEquals(stopwatch.getActive(), 0);
		// there is no change because this split was started on disabled stopwatch
		Assert.assertEquals(stopwatch.getTotal(), total);
		Assert.assertEquals(stopwatch.getCounter(), counter);
		assertStopwatchAndSampleAreEqual(stopwatch);
	}

	@Test
	public void sampling() {
		Stopwatch stopwatch = SimonManager.getStopwatch(null);
		stopwatch.addSplit(Split.create(10));
		assertStopwatchAndSampleAreEqual(stopwatch, stopwatch.sampleIncrement(""));
		// no change, zero increment
		assertZeroSample(stopwatch.sampleIncrement(""));

		stopwatch.addSplit(Split.create(10));
		assertIncrementalSampleAfterIncrease(stopwatch.sampleIncrement(""));

		// another change produces the same incremental sample
		stopwatch.addSplit(Split.create(10));
		assertIncrementalSampleAfterIncrease(stopwatch.sampleIncrement(""));

		// after key is removed, next incremental sample equals normal sample
		Assert.assertTrue(stopwatch.stopIncrementalSampling(""));
		stopwatch.addSplit(Split.create(10));
		assertStopwatchAndSampleAreEqual(stopwatch, stopwatch.sampleIncrement(""));
		assertStopwatchAndSampleAreEqual(stopwatch);

		// check of return value for nonexistent increment key
		Assert.assertFalse(stopwatch.stopIncrementalSampling("nonexistent"));
	}

	private void assertIncrementalSampleAfterIncrease(StopwatchSample sample) {
		Assert.assertEquals(sample.getTotal(), 10);
		Assert.assertEquals(sample.getCounter(), 1);
		Assert.assertEquals(sample.getMax(), 10);
		Assert.assertEquals(sample.getMin(), 10);
		Assert.assertEquals(sample.getLast(), 10);
	}

	private void assertZeroSample(StopwatchSample sample) {
		Assert.assertEquals(sample.getTotal(), 0);
		Assert.assertEquals(sample.getCounter(), 0);
		Assert.assertEquals(sample.getMax(), 0);
		Assert.assertEquals(sample.getMin(), Long.MAX_VALUE);
		Assert.assertEquals(sample.getFirstUsage(), 0);
		Assert.assertEquals(sample.getLastUsage(), 0);
	}

	private void assertStopwatchAndSampleAreEqual(Stopwatch stopwatch) {
		assertStopwatchAndSampleAreEqual(stopwatch, stopwatch.sample());
	}

	private void assertStopwatchAndSampleAreEqual(Stopwatch stopwatch, StopwatchSample sample) {
		Assert.assertEquals(sample.getTotal(), stopwatch.getTotal());
		Assert.assertEquals(sample.getCounter(), stopwatch.getCounter());
		Assert.assertEquals(sample.getMax(), stopwatch.getMax());
		Assert.assertEquals(sample.getMin(), stopwatch.getMin());
		Assert.assertEquals(sample.getFirstUsage(), stopwatch.getFirstUsage());
		Assert.assertEquals(sample.getLastUsage(), stopwatch.getLastUsage());
		Assert.assertEquals(sample.getMaxTimestamp(), stopwatch.getMaxTimestamp());
		Assert.assertEquals(sample.getMinTimestamp(), stopwatch.getMinTimestamp());
		Assert.assertEquals(sample.getActive(), stopwatch.getActive());
		Assert.assertEquals(sample.getMaxActive(), stopwatch.getMaxActive());
		Assert.assertEquals(sample.getMaxActiveTimestamp(), stopwatch.getMaxActiveTimestamp());
		Assert.assertEquals(sample.getLast(), stopwatch.getLast());
		Assert.assertEquals(sample.getMean(), stopwatch.getMean());
		Assert.assertEquals(sample.getStandardDeviation(), stopwatch.getStandardDeviation());
		Assert.assertEquals(sample.getVariance(), stopwatch.getVariance());
		Assert.assertEquals(sample.getVarianceN(), stopwatch.getVarianceN());
	}
}
