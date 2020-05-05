public class Bullet {
    float x;
    float y;
    int damage;
    float speed;
    Vector2D vector;
    Vector2D position;
    float angle;
    int ownerID;
    public Bullet(float x, float y, int damage, float speed, Vector2D vector, Vector2D pos, float angle, int ownerID) {
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.speed = speed;
        this.vector = vector;
        this.angle = angle;
        this.ownerID = ownerID;
        position = pos;
    }
}
