public class Bullet {
    float x;
    float y;
    int damage;
    float speed;
    Vector2D vector;
    Vector2D position;
    public Bullet(float x, float y, int damage, float speed, Vector2D vector, Vector2D pos) {
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.speed = speed;
        this.vector = vector;
        position = pos;
    }
}
