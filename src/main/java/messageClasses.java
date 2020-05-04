
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.util.ArrayList;

public class messageClasses {
    public static void registerClasses(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(mapRequest.class);
        kryo.register(mapReceive.class);
        kryo.register(playerUpdate.class);
        kryo.register(serverUpdate.class);
        kryo.register(ArrayList.class);
        kryo.register(User.class);
        kryo.register(Projectile.class);
        kryo.register(Bullet.class);
        kryo.register(Vector2D.class);
    }

    static public class mapRequest {
        String name;

        public mapRequest(String name) {
            this.name = name;
        }

        public mapRequest() {
        }
    }

    static public class mapReceive {
        public String mapName;
    }

    static public class playerUpdate {
        float x;
        float y;

        public playerUpdate(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public playerUpdate() {
        }
    }

    static public class serverUpdate {
        ArrayList<User> userList;
        ArrayList<Bullet> bulletList;

        public serverUpdate(ArrayList<User> list, ArrayList<Bullet> bullets) {
            userList = list;
            bulletList = bullets;
        }

        public serverUpdate() {
        }
    }

    static public class Projectile {
        public float locationX;
        public float locationY;
        public float targetX;
        public float targetY;
        public int damage;
        public float velocity;

        public String texture;
    }
}
