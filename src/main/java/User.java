public class User {
    int ID;
    float x;
    float y;
    String username;
    boolean alive = false;
    int role;
    public User(int ID, float x, float y, String username) {
        this.ID = ID;
        this.x = x;
        this.y = y;
        this.username = username;
    }
    public User(){}
}
