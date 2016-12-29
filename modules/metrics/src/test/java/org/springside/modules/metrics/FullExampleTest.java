package org.springside.modules.metrics;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.springside.modules.metrics.exporter.JmxExporter;
import org.springside.modules.metrics.metric.Counter;
import org.springside.modules.metrics.metric.Histogram;
import org.springside.modules.metrics.metric.Timer;
import org.springside.modules.metrics.metric.Timer.TimerContext;
import org.springside.modules.metrics.reporter.ConsoleReporter;
import org.springside.modules.metrics.reporter.Slf4jReporter;

/**
 * 示例分别使用ConsoleReporter与Slf4jReporter, 演示Counter, Histogram 和 Timer的用法.
 * 
 * src/test/resources/logback.xml中已对Slf4jReporter进行配置
 */
public class FullExampleTest {

	@Test
	public void counterExample() throws InterruptedException {
		MetricRegistry metricRegistry = new MetricRegistry();

		// 使用ConsoleReporter
		ConsoleReporter consoleReporter = new ConsoleReporter();
		Counter counter = metricRegistry.counter(MetricRegistry.name("UserService", "getUser.counter"));

		ReportScheduler scheduler = new ReportScheduler(metricRegistry, consoleReporter);
		scheduler.start(1, TimeUnit.SECONDS);

		counter.inc();
		Thread.sleep(1050);

		counter.inc(2);
		Thread.sleep(1050);
		scheduler.stop();
	}

	@Test
	public void histogramExample() throws InterruptedException {
		MetricRegistry metricRegistry = new MetricRegistry();

		// 使用slf4j reporter,并使用默认logger名字
		Slf4jReporter slf4jReporter = new Slf4jReporter();

		ReportScheduler scheduler = new ReportScheduler(metricRegistry, slf4jReporter);
		scheduler.start(1, TimeUnit.SECONDS);

		Histogram histogram = metricRegistry.histogram(MetricRegistry.name("UserService", "getUser.latency"));
		histogram.update(1);
		histogram.update(100);

		Thread.sleep(1050);
		// 增加百分位
		histogram.setPcts(new Double[] { 0.99d, 0.999d });
		histogram.update(2);
		histogram.update(200);
		Thread.sleep(1050);
		scheduler.stop();
	}

	@Test
	public void timerExample() throws InterruptedException {
		MetricRegistry metricRegistry = new MetricRegistry();
		// 使用slf4j reporter,并使用自定义logger名字
		Slf4jReporter slf4jReporter = new Slf4jReporter("mymetrics");

		ReportScheduler scheduler = new ReportScheduler(metricRegistry);
		scheduler.addReporter(slf4jReporter);

		scheduler.start(1, TimeUnit.SECONDS);

		Timer timer = metricRegistry.timer(MetricRegistry.name("UserService", "getUser.timer"),
				new Double[] { 0.99d, 0.999d });

		// 写法1
		TimerContext timerContext = timer.start();
		Thread.sleep(100);
		timerContext.stop();

		timerContext = timer.start();
		Thread.sleep(200);
		timerContext.stop();

		Thread.sleep(750);

		// 用法2
		long start = System.currentTimeMillis();
		Thread.sleep(150);
		timer.update(start);

		start = System.currentTimeMillis();
		Thread.sleep(250);
		timer.update(start);

		Thread.sleep(650);
		scheduler.stop();

	}

	@Test
	public void testJmx() throws InterruptedException, Exception {

		MetricRegistry metricRegistry = new MetricRegistry();

		Counter counter = metricRegistry.counter(MetricRegistry.name("UserService", "getUserWithJmx.counter"));

		//无reporter，only exporter
		JmxExporter jmxExporter = new JmxExporter("example", metricRegistry);
		jmxExporter.initMBeans();

		ReportScheduler scheduler = new ReportScheduler(metricRegistry);
		scheduler.start(1, TimeUnit.SECONDS);

		counter.inc();
		Thread.sleep(1050);

		counter.inc(2);
		Thread.sleep(1050);
		scheduler.stop();

		// 校验MBean中的值
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		System.out.println("TotalCount from MBean:" + mBeanServer
				.getAttribute(new ObjectName("example", "name", "UserService.getUserWithJmx.counter"), "TotalCount")
				.toString());

	}

}
