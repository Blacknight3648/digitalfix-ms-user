package df.digitalfix_ms_user;

import org.springframework.boot.SpringApplication;

public class TestDigitalfixMsUserApplication {

	public static void main(String[] args) {
		SpringApplication.from(DigitalfixMsUserApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
