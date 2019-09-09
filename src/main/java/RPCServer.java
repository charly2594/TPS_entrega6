import com.rabbitmq.client.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;


public class RPCServer {

    private static final String RPC_QUEUE_NAME = "rpc_queue";

    private static int fib(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib(n - 1) + fib(n - 2);
    }

    //Testing card API
    public static String searchById(String Id){
        System.out.println("-----searchById:input: " + Id);
        Client client = ClientBuilder.newBuilder().build();
        System.out.println("-----searchById:client: " + client);
        WebTarget target = client.target("https://db.ygoprodeck.com/api/v5/cardinfo.php")
                .queryParam("fname", Id);
        String response = target.request().get().readEntity(String.class);;//.request(MediaType.APPLICATION_JSON).toString();
        //System.out.println("-----searchById:response: " + response);
        return response;
    }




        public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("crane.rmq.cloudamqp.com");
        factory.setUsername("riikuyvl");
        factory.setVirtualHost("riikuyvl");
        factory.setPassword("WtYUU4rdx0-UOTPE0yrObjMZt4WXuAxh");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);

            channel.basicQos(1);

            System.out.println(" [x] Awaiting RPC requests");

            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                String response = "";

                try {
                    String message = new String(delivery.getBody(), "UTF-8");
                    //int n = Integer.parseInt(message);

                    //System.out.println(" [.] fib(" + message + ")");
                    //response += fib(n);
                    System.out.println(" [.] id or name of card: " + message);
                    response = searchById(message);
                    System.out.println(" [.] response: " + response);
                } catch (RuntimeException e) {
                    System.out.println(" [.] " + e.toString());
                    String message = new String(delivery.getBody(), "UTF-8");
                } finally {
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    // RabbitMq consumer worker thread notifies the RPC server owner thread
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };

            channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> { }));
            // Wait and be prepared to consume the message from RPC client.
            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}