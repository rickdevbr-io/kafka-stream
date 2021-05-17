package br.edu.rickdev.kafkastream;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.ForeachAction;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("deprecation")
@SpringBootApplication
@EnableBinding(kafkaBinding.class)
public class KafkaStreamApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaStreamApplication.class, args);
	}
	
	public static class PageViewEventSource implements ApplicationRunner{
  
		private final MessageChannel pageViewsOut = null;
		private final Log log = LogFactory.getLog(getClass());
		
		@Override
		public void run(ApplicationArguments args) throws Exception {
			List<String> names = Arrays.asList("jLong","pwebb","shacko", "abilan", "ozhurakoushy", "grussell");
			List<String> pages = Arrays.asList("blog","sitemap","initializr", "news", "colophon", "about");
			Runnable runnable = () -> {
				String rName = pages.get(new Random().nextInt(names.size()));
				String rPage = pages.get(new Random().nextInt(pages.size()));
				PageViewEvent pageViewEvent = new PageViewEvent(rName, rPage, Math.random() > .5 ? 10 : 1000);
				Message<PageViewEvent> message = MessageBuilder
						.withPayload(pageViewEvent)
						.setHeader(KafkaHeaders.MESSAGE_KEY, pageViewEvent.getUserId().getBytes())
						.build();
				try {
					this.pageViewsOut.send(message);
					log.info("send message" + message);
				}catch(Exception e) {
					log.info(e.toString());
				}
			};
			Executors.newScheduledThreadPool(1).scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);
		}
		
	}

	@Component
	public static class PageViewEventProcessor{
		
		@StreamListener
		@SendTo(kafkaBinding.PAGE_COUNT_OUT)
		public KStream<String, Long> process (@Input(kafkaBinding.PAGE_VIEWS_IN) KStream<String, PageViewEvent> events) {
			return events
					.filter((key, value) -> value.getDuration() > 10)
					.map((key, value) -> new KeyValue<>(value.getPage(), "0"))
					.groupByKey()
					.count(Materialized.as(kafkaBinding.PAGE_COUNT_MV))
					.toStream();
		}
	}
	
	@Component
	public static class PageCountSink{
		
		private final Log log = LogFactory.getLog(getClass());
		
		@StreamListener
		public void process(@Input((kafkaBinding.PAGE_COUNT_IN)) KTable<String, Long> counts) {
			counts
			.toStream()
			.foreach(new ForeachAction<String, Long>(){
				@Override
				public void apply(String key, Long value) {
					log.info(key + "=" + value);
				}
			});
		}
	}
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class PageViewEvent{
	private String userId, page;
	private long duration;
}

interface kafkaBinding{
	
	String PAGE_VIEWS_OUT = "pageViewsOut";
	String PAGE_VIEWS_IN = "pageViewsIn";
	String PAGE_COUNT_MV = "pageCountMv";
	String PAGE_COUNT_OUT = "pageCountOut";
	String PAGE_COUNT_IN = "pageCountIn";
	
	@SuppressWarnings("deprecation")
	@Output(PAGE_VIEWS_OUT)
	MessageChannel pageViewsOut();
	
	@SuppressWarnings("deprecation")
	@Input(PAGE_VIEWS_IN)
	KStream<String, PageViewEvent> pageViewsIn();
	
	@SuppressWarnings("deprecation")
	@Output(PAGE_COUNT_OUT)
	KStream<String, Long> pageCountOut();
	
	@SuppressWarnings("deprecation")
	@Output(PAGE_COUNT_IN)
	KTable<String, Long> pageCountIn();
	
}