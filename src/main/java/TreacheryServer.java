import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TreacheryServer {
    Server server = new Server();
    ArrayList<User> userList = new ArrayList<>();
    ArrayList<User> tempList = new ArrayList<>();
    ArrayList<Bullet> bullets = new ArrayList<>();
    User currentUser = new User();

    public TreacheryServer() {
        messageClasses.registerClasses(server);

        server.start();
        try {
            server.bind(54555);
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof messageClasses.mapRequest) {
                    messageClasses.mapRequest message = (messageClasses.mapRequest) object;
                    messageClasses.mapReceive response = new messageClasses.mapReceive();
                    response.mapName = "map1.tmx";
                    connection.sendTCP(response);
                    userList.add(new User(connection.getID(), 0, 0, message.name));
                } else if (object instanceof messageClasses.playerUpdate) {
                    messageClasses.playerUpdate message = (messageClasses.playerUpdate) object;
                    for (User u : userList) {
                        if (u.ID == connection.getID()) {
                            currentUser = u;
                            u.x = message.x;
                            u.y = message.y;
                        }
                    }
                    tempList.clear();
                    tempList.addAll(userList);
                    tempList.removeIf(user -> user.ID == connection.getID());
                    connection.sendTCP(new messageClasses.serverUpdate(tempList, bullets));

                } else if (object instanceof messageClasses.Projectile) {
                    messageClasses.Projectile m = (messageClasses.Projectile) object;
                    Vector2D start_pos = new Vector2D(m.locationX, m.locationY);
                    Vector2D end_pos = new Vector2D(m.targetX, m.targetY);
                    System.out.println(start_pos + ", " + end_pos);
//                    Vector2D vector = start_pos.getSubtracted(end_pos);
                    Vector2D vector = end_pos.getSubtracted(start_pos);
                    vector.normalize();

                    bullets.add(new Bullet(m.locationX, m.locationY, m.damage, m.velocity, vector, start_pos));
                }
            }
        });
        server.addListener(new Listener() {
            public void disconnected(Connection connection) {
                userList.removeIf(user -> user.ID == connection.getID());
                if(userList.isEmpty()) bullets.clear();
            }
        });
        Runnable update = () -> {
            for (Bullet b: bullets) {
                Vector2D v = new Vector2D(b.vector);
                v.multiply(b.speed);
                b.position.add(v);
                b.x = (float) b.position.x;
                b.y = (float) b.position.y;
            }

        };
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(update, 0, 100, TimeUnit.MILLISECONDS);
    }
}
