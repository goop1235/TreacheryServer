public class Bullet {
    float x;
    float y;
    int damage;
    float speed;
    Vector2D vector;
    Vector2D position;
    float angle;
    int ownerID;
    String texture;
    public Bullet(float x, float y, int damage, float speed, Vector2D vector, Vector2D pos, float angle, int ownerID, String texture) {
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.speed = speed;
        this.vector = vector;
        this.angle = angle;
        this.texture = texture;
        this.ownerID = ownerID;
        position = pos;
    }
    public Bullet(){}
}
