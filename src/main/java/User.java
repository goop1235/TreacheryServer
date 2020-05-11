public class User {
    int ID;
    float x;
    float y;
    String username;
    boolean alive = false;
    int role;
    boolean showName;
    String texture;
    public User(int ID, float x, float y, String username, boolean showName, String texture) {
        this.ID = ID;
        this.x = x;
        this.y = y;
        this.username = username;
        this.showName = showName;
        this.texture = texture;
    }
    public User(){}
}
