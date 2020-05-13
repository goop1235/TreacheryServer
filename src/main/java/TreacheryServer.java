import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import org.mapeditor.core.Map;
import org.mapeditor.core.MapObject;
import org.mapeditor.core.ObjectGroup;
import org.mapeditor.io.TMXMapReader;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TreacheryServer {
    Map map;
    int MAP_HEIGHT;
    int MAP_WIDTH;
    final String mapName = "map2.tmx";
    Server server = new Server();
    ArrayList<User> userList = new ArrayList<>();
    ArrayList<User> tempList = new ArrayList<>();
    ArrayList<Bullet> bullets = new ArrayList<>();
    ArrayList<Bullet> bulletsRemove = new ArrayList<>();
    User currentUser = new User();


    public final int MID_ROUND = 1;
    public final int WAITING = 2;
    public final int INNOCENT = 1;
    public final int TRAITOR = 2;
    public int gameState = WAITING;
    public int roundCooldown = 0;

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
                    response.list = new ArrayList<>();
                    for (MapObject o : ((ObjectGroup) map.getLayer(0)).getObjects()) {
                        response.list.add(o.getBounds());
                    }
                    connection.sendTCP(response);
                    userList.add(new User(connection.getID(), 0, 0, message.name, true, "ers"));

                } else if (object instanceof messageClasses.playerUpdate) {
                    messageClasses.playerUpdate message = (messageClasses.playerUpdate) object;
                    for (User u : userList) {
                        if (u.ID == connection.getID()) {
                            currentUser = u;
                            if (message.alive) {
                                u.x = message.x;
                                u.y = message.y;
                                u.showName = message.showName;
                                u.texture = message.texture;
                            }

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
                    float angle = (float) Math.atan2(m.targetY - m.locationY, m.targetX - m.locationX);
                    angle = (float) Math.toDegrees(angle);
                    bullets.add(new Bullet(m.locationX, m.locationY, m.damage, m.velocity, vector, start_pos, angle, connection.getID(), m.texture, m.width, m.height, m.rotate,
                            m.collision, m.removeOnHit));
                } else if (object instanceof messageClasses.Death) {
                    messageClasses.Death m = (messageClasses.Death) object;
                    for (Connection c : server.getConnections()) {
                        server.sendToTCP(c.getID(), m);
                    }
                    for (User u : userList) {
                        if (u.ID == connection.getID()) u.alive = false;
                    }
                } else if (object instanceof messageClasses.ItemDropped) {
                    for (Connection c : server.getConnections()) {
                        if (c.getID() != connection.getID()) server.sendToTCP(c.getID(), object);
                    }
                } else if (object instanceof messageClasses.ItemPickedUp) {
                    for (Connection c : server.getConnections()) {
                        if (c.getID() != connection.getID()) server.sendToTCP(c.getID(), object);
                    }
                }

            }
        });
        server.addListener(new Listener() {
            public void disconnected(Connection connection) {
                userList.removeIf(user -> user.ID == connection.getID());
                if (userList.isEmpty()) bullets.clear();
            }
        });
        // Updates projectiles 20 times per second
        Runnable update = () -> {
            try {
                update();
            } catch (Throwable t) {  // Catch Throwable rather than Exception (a subclass).
                System.out.println("Caught exception in ScheduledExecutorService. StackTrace:\n" + Arrays.toString(t.getStackTrace()));
            }

        };
        // Load map
        try {
            TMXMapReader mapReader = new TMXMapReader();
            map = mapReader.readMap("maps/" + mapName);
            MAP_WIDTH = map.getWidth() * 64;
            MAP_HEIGHT = map.getHeight() * 64;
            for (MapObject o : ((ObjectGroup) map.getLayer(0)).getObjects()) {
                o.setY(MAP_HEIGHT - o.getY() - o.getHeight());
            }
        } catch (Throwable e) {
            System.out.println("Error while reading the map:\n" + e.getMessage());
            return;
        }


        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(update, 0, 50, TimeUnit.MILLISECONDS);
    }

    // Runs 50 times per second
    public void update() {
        // Check if should start game
        if (gameState == WAITING) {
            roundCooldown--;
            if (server.getConnections().length >= 2 && roundCooldown <= 0) {
                startRound();
                gameState = MID_ROUND;
            }
            // Check if someone won
        } else if (gameState == MID_ROUND) {
            int innocents = 0;
            int traitors = 0;
            for (User user : userList) {
                if (user.alive) {
                    if (user.role == TRAITOR) traitors++;
                    else if (user.role == INNOCENT) innocents++;
                }
            }
            if (innocents == 0) {
                for (Connection connection : server.getConnections()) {
                    server.sendToTCP(connection.getID(), new messageClasses.RoundEnd(TRAITOR, userList));
                }
                gameState = WAITING;
                roundCooldown = 100;
            } else if (traitors == 0) {
                for (Connection connection : server.getConnections()) {
                    server.sendToTCP(connection.getID(), new messageClasses.RoundEnd(INNOCENT, userList));
                }
                gameState = WAITING;
                roundCooldown = 100;
            }

            // Update bullets
            bulletsRemove.clear();
            for (Bullet b : bullets) {
                Rectangle rectangle = new Rectangle((int) b.x, (int) b.y, b.width, b.height);
                if (rectangle.width == 0) {
                    rectangle.width = 1;
                    rectangle.height = 1;
                }

                Vector2D v = new Vector2D(b.vector);
                v.multiply(b.speed);
                b.position.add(v);
                b.x = (float) b.position.x;
                b.y = (float) b.position.y;

                if (b.x < 0 || b.y < 0 || b.x > MAP_WIDTH || b.y > MAP_HEIGHT) {
                    bulletsRemove.add(b);
                }
                if (b.collision) {

                    List<MapObject> objects = ((ObjectGroup) map.getLayer(0)).getObjects();
                    for (MapObject o : objects) {
                        boolean wall = false;
                        try {
                            wall = Boolean.parseBoolean(o.getProperties().getProperty("Wall"));
                        } catch (Exception ignored) {
                        }

                        if (b.width == 0 || b.height == 0) {
                            if ((o.getBounds().contains(b.x, b.y)) && wall) {
                                bulletsRemove.add(b);
                            }
                        } else {
                            if ((o.getBounds().intersects(rectangle) || rectangle.intersects(o.getBounds())) && wall) {
                                bulletsRemove.add(b);
                            }
                        }
                    }
                }

                for (User u : userList) {
                    Rectangle userRect = new Rectangle((int)u.x, (int)u.y, 50, 50);
                    if (b.ownerID != u.ID && !b.playersHit.contains(u.ID) && (userRect.intersects(rectangle) || rectangle.intersects(userRect))) {
                        server.sendToTCP(u.ID, new messageClasses.Hit(b.damage));
                        b.playersHit.add(u.ID);
                        if (b.removeOnHit) bulletsRemove.add(b);
                    }
                }
            }
            bullets.removeAll(bulletsRemove);
        }
    }

    public void startRound() {
        bullets.clear();
        int numTraitors = (int) Math.ceil((float) server.getConnections().length / 4f);
        int role;
        List<Connection> list = new ArrayList<>();
        for (Connection connection : server.getConnections()) {
            list.add(connection);
        }
        Collections.shuffle(list);

        for (Connection c : list) {
            if (numTraitors > 0) {
                numTraitors--;
                role = TRAITOR;

            } else role = INNOCENT;

            server.sendToTCP(c.getID(), new messageClasses.RoundStart(role));
            for (User user : userList) {
                if (user.ID == c.getID()) user.role = role;
            }
        }
        for (User u : userList) {
            u.alive = true;
        }
    }


}
