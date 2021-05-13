package br.edu.rickdev.kafkastream;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("deprecation")
@SpringBootApplication
@EnableBinding(kafkaBinding.class)
public class KafkaStreamApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaStreamApplication.class, args);
	}
	
	public static class PageViewEventSource implements ApplicationRunner{

		private final MessageChannel pageViewsOut = null;
		
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
	
	@SuppressWarnings("deprecation")
	@Output(PAGE_VIEWS_OUT)
	MessageChannel pageViewsOut();
}