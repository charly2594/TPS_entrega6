import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

public class RPCClient implements AutoCloseable {

    private Connection connection;
    private Channel channel;
    private String requestQueueName = "ver_informacion_basica";

    public RPCClient() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("crane.rmq.cloudamqp.com");
        factory.setUsername("riikuyvl");
        factory.setVirtualHost("riikuyvl");
        factory.setPassword("WtYUU4rdx0-UOTPE0yrObjMZt4WXuAxh");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public static void main(String[] argv) {
        try (RPCClient fibonacciRpc = new RPCClient()) {
            //for (int i = 0; i < 32; i++) {
                //String i_str = Integer.toString(i);
                //System.out.println(" [x] Requesting fib(" + i_str + ")");
                //String response = fibonacciRpc.call(i_str);
                //System.out.println(" [.] Got '" + response + "'");
            /*
            for (int i = 1; i < 4; i++) {
                String i_str = Integer.toString(i);
                System.out.println(i_str);
                String response = fibonacciRpc.call("harpie lady " + i_str);
                System.out.println(" [.] Got '" + response + "'");
            }
            */
            String msg = "Neos Fusion";
            String response = fibonacciRpc.call(msg);
            System.out.println(" [.] Got '" + response + "'");
        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String call(String message) throws IOException, InterruptedException {
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.take();
        channel.basicCancel(ctag);
        return result;
    }

    public void close() throws IOException {
        connection.close();
    }
}