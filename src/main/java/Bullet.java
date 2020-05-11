import java.util.ArrayList;

public class Bullet {
    float x;
    float y;
    int width;
    int height;
    int damage;
    float speed;
    Vector2D vector;
    Vector2D position;
    float angle;
    int ownerID;
    String texture;
    boolean rotate;
    boolean collision;
    boolean removeOnHit;
    ArrayList<Integer> playersHit = new ArrayList<>();

    public Bullet(float x, float y, int damage, float speed, Vector2D vector, Vector2D pos, float angle, int ownerID, String texture, int width, int height, boolean rotate,
                  boolean collision, boolean removeOnHit) {
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.speed = speed;
        this.vector = vector;
        this.angle = angle;
        this.texture = texture;
        this.ownerID = ownerID;
        position = pos;
        this.width = width;
        this.height = height;
        this.rotate = rotate;
        this.collision = collision;
        this.removeOnHit = removeOnHit;
    }
    public Bullet(){}
}
