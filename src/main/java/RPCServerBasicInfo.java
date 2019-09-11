import com.rabbitmq.client.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

//import javax.json.Json;
//import javax.json.JsonObject;
//import javax.json.JsonReader;

public class RPCServerBasicInfo {

    private static final String RPC_QUEUE_basic_info = "ver_informacion_basica";
    private static final String RPC_QUEUE_full_info = "ver_informacion_detallada";
    private static final String RPC_QUEUE_fuzzy = "busqueda_parcial";

    private static int fib(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib(n - 1) + fib(n - 2);
    }

    //API request and simplification
    public static String searchById(String Id){
        System.out.println("-----searchById:input: " + Id);
        Client client = ClientBuilder.newBuilder().build();
        System.out.println("-----searchById:client: " + client);
        WebTarget target = client.target("https://db.ygoprodeck.com/api/v5/cardinfo.php")
                .queryParam("name", Id.toString());
        System.out.println("-----searchById:webTarget: " + target.getUri());
        String response = target.request().get().readEntity(String.class);
        System.out.println("-----response: " + response);
        //Transforming String to Json and removing List brackets:
        JSONObject obj = new JSONObject(response.substring(1, response.length() - 1));
        System.out.println(obj.getString("name"));

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("name", obj.get("name"));
        jsonResponse.put("id", obj.get("id"));
        jsonResponse.put("image_url_small", ( (JSONObject)(
                ( (JSONArray) obj.get("card_images"))
                        .getJSONObject(0) ) )
                .get("image_url_small"));
        //System.out.println("--------digging: "+  ( (JSONObject)( ( (JSONArray) obj.get("card_images")).getJSONObject(0) ) ).get("image_url_small"));


        return jsonResponse.toString();
    }




    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("crane.rmq.cloudamqp.com");
        factory.setUsername("riikuyvl");
        factory.setVirtualHost("riikuyvl");
        factory.setPassword("WtYUU4rdx0-UOTPE0yrObjMZt4WXuAxh");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(RPC_QUEUE_basic_info, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_basic_info);

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
                    //System.out.println(" [.] response: " + response);
                } catch (RuntimeException e) {
                    System.out.println(" [.] error-->" + e.toString());
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

            channel.basicConsume(RPC_QUEUE_basic_info, false, deliverCallback, (consumerTag -> { }));
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