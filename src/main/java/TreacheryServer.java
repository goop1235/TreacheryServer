import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import org.mapeditor.core.Map;
import org.mapeditor.core.MapObject;
import org.mapeditor.core.ObjectGroup;
import org.mapeditor.io.TMXMapReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TreacheryServer {
    Map map;
    int MAP_HEIGHT;
    int MAP_WIDTH;
    final String mapName = "map1.tmx";
    Server server = new Server();
    ArrayList<User> userList = new ArrayList<>();
    ArrayList<User> tempList = new ArrayList<>();
    ArrayList<Bullet> bullets = new ArrayList<>();
    ArrayList<Bullet> bulletsRemove = new ArrayList<>();
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
                    response.mapName = mapName;
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
                    Vector2D vector = end_pos.getSubtracted(start_pos);
                    vector.normalize();
                    float angle = (float) Math.atan2(m.targetY- m.locationY, m.targetX- m.locationX);
                    angle = (float) Math.toDegrees(angle);
                    bullets.add(new Bullet(m.locationX, m.locationY, m.damage, m.velocity, vector, start_pos, angle, connection.getID()));
                }
            }
        });
        server.addListener(new Listener() {
            public void disconnected(Connection connection) {
                userList.removeIf(user -> user.ID == connection.getID());
                if(userList.isEmpty()) bullets.clear();
            }
        });
        // Updates projectiles 20 times per second
        Runnable update = () -> {
            bulletsRemove.clear();
            for (Bullet b: bullets) {
                Vector2D v = new Vector2D(b.vector);
                v.multiply(b.speed);
                b.position.add(v);
                b.x = (float) b.position.x;
                b.y = (float) b.position.y;

                if (b.x < 0 || b.y < 0 || b.x > MAP_WIDTH || b.y > MAP_HEIGHT) {
                    bulletsRemove.add(b);
                }
                ObjectGroup objects = (ObjectGroup) map.getLayer(1);
                for (MapObject o: objects) {
                    if (o.getBounds().contains(b.x, b.y)) bulletsRemove.add(b);
                }

                for (User u: userList) {
                    if (b.ownerID != u.ID && b.x > u.x && b.x < u.x + 50 && b.y > u.y && b.y < u.y + 50) {
                        server.sendToTCP(u.ID, new messageClasses.Hit(b.damage));
                        bulletsRemove.add(b);
                    }
                }
            }
            bullets.removeAll(bulletsRemove);

        };
        // Load map
        try {
            TMXMapReader mapReader = new TMXMapReader();
            map = mapReader.readMap("maps/" + mapName);
            MAP_WIDTH = map.getHeight() * 64;
            MAP_HEIGHT = map.getWidth() * 64;
        } catch (Exception e) {
            System.out.println("Error while reading the map:\n" + e.getMessage());
            return;
        }



        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(update, 0, 50, TimeUnit.MILLISECONDS);
    }
}
