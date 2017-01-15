package com.kitchen.timer;

import java.util.Timer;
import java.util.TimerTask;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class KitchenTimerScheduler {

	private long taskDuration;
	private final static String COOKING_TIMER_TABLE = "Cooking_Timer";
	private final static String CUSTOMER_ID = "Customer_ID";
	private final static String FOOD_ID = "Food_Item";

	public KitchenTimerScheduler(String taskName, String durationInMilis, String customerId, String userId,
			String foodId, String foodItem) {

		this.taskDuration = Long.parseLong(durationInMilis);

		System.out.println("Timer for " + taskName + " has started.");

		TimerTask timerTask = new TimerTask() {

			int counter = 0;

			@Override
			public void run() {
				System.out.println("TimerTask executing counter is: " + counter);
				counter++;// increments the counter
				final String FROM = "cdeeran@gmail.com"; // Replace with your
															// "From"
				// address. This address
				// must be verified.
				final String TO = "cdeeran@gmail.com"; // Replace with a "To"
				// address. If your
				// account is still in
				// the
				// sandbox, this address
				// must be verified.
				final String BODY = taskName
						+ " timer has successfully completed, and has been removed from the database.";
				final String SUBJECT = taskName + "Timer Finished!";

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

					// Instantiate an Amazon SES client, which will make the
					// service
					// call.
					// The service call requires your AWS credentials.
					// Because we're not providing an argument when
					// instantiating the
					// client, the SDK will attempt to find your AWS credentials
					// using the default credential provider chain. The first
					// place the
					// chain looks for the credentials is in environment
					// variables
					// AWS_ACCESS_KEY_ID and AWS_SECRET_KEY.
					// For more information, see
					// http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
					AmazonSimpleEmailServiceClient client = new AmazonSimpleEmailServiceClient();
					// Send the email.
					client.sendEmail(request);
					System.out.println("Email sent successfully!");
				} catch (Exception ex) {
					System.out.println("The email was not sent.");
					System.out.println("Error message: " + ex.getMessage());
				}

				System.out.println("Deleteing timer: " + taskName + " from database......");

				deleteTask(userId, foodItem);
			}
		};

		Timer timer = new Timer(taskName);// create a new Timer

		timer.schedule(timerTask, taskDuration);// this line
																// starts the
		// timer at the same
		// time its executed

		//
		// Runnable task = () -> {
		//
		// System.out.println("Timer for " + taskName + " has completed.");
		//
		// };

	}

	private void deleteTask(String user, String food) {

		AmazonDynamoDBClient timerDb = new AmazonDynamoDBClient();
		DynamoDB dynamoDb = new DynamoDB(timerDb);
		Table dbTable = dynamoDb.getTable(COOKING_TIMER_TABLE);
		DeleteItemOutcome itemResults = dbTable.deleteItem(CUSTOMER_ID, user, FOOD_ID, food);

		if (itemResults.getDeleteItemResult() != null) {
			System.out.println("Deletion successfull!");
		} else {
			System.out.println("Deletion failed!");
		}

	}

}
