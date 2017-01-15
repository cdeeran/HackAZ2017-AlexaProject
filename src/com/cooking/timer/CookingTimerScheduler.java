package com.cooking.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class CookingTimerScheduler {

	private long taskDuration;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public CookingTimerScheduler(String taskName, String durationInMilis) {

		this.taskDuration = Long.parseLong(durationInMilis);

		System.out.println("Timer for " + taskName + " has started.");

		Runnable task = () -> {

			System.out.println("Timer for " + taskName + " has completed.");

		};

		scheduler.schedule(task, taskDuration, TimeUnit.MILLISECONDS);

		final String FROM = "cdeeran@gmail.com"; // Replace with your "From"
													// address. This address
													// must be verified.
		final String TO = "cdeeran@gmail.com"; // Replace with a "To"
												// address. If your
												// account is still in
												// the
												// sandbox, this address
												// must be verified.
		final String BODY = task + " timer has successfully completed!";
		final String SUBJECT = task + "Timer Finished!";

		// Construct an object to contain the recipient address.
		Destination destination = new Destination().withToAddresses(new String[] { TO });

		// Create the subject and body of the message.
		Content subject = new Content().withData(SUBJECT);
		Content textBody = new Content().withData(BODY);
		Body body = new Body().withText(textBody);

		// Create a message with the specified subject and body.
		Message message = new Message().withSubject(subject).withBody(body);

		// Assemble the email.
		SendEmailRequest request = new SendEmailRequest().withSource(FROM).withDestination(destination)
				.withMessage(message);

		try {
			System.out.println("Attempting to send email to: " + TO);

			// Instantiate an Amazon SES client, which will make the service
			// call.
			// The service call requires your AWS credentials.
			// Because we're not providing an argument when instantiating the
			// client, the SDK will attempt to find your AWS credentials
			// using the default credential provider chain. The first place the
			// chain looks for the credentials is in environment variables
			// AWS_ACCESS_KEY_ID and AWS_SECRET_KEY.
			// For more information, see
			// http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
			AmazonSimpleEmailServiceClient client = new AmazonSimpleEmailServiceClient();
			// Send the email.
			client.sendEmail(request);
			System.out.println("Email sent!");
		} catch (Exception ex) {
			System.out.println("The email was not sent.");
			System.out.println("Error message: " + ex.getMessage());
		}
	}

}
