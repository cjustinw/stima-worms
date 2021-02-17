package za.co.entelect.challenge.command;

import za.co.entelect.challenge.enums.Direction;

public class SelectCommand implements Command {

    private final int wormID;
    private final String command;
    private int x;
    private int y;
    private Direction direction;

    public SelectCommand(int wormID, String command, Direction direction){
       this.wormID = wormID;
       this.command = command;
       this.direction = direction;
    }

    public SelectCommand(int wormID, String command, int x, int y ){
        this.wormID = wormID;
        this.command = command;
        this.x = x;
        this.y = y;
    }

    @Override
    public String render() {
        if(command.equals("shoot")){
            return String.format("select %s;shoot %s", wormID, direction ) ;
        }
        else if(command.equals("snowball")){
            return String.format("select %s;snowball %d %d", wormID, x, y ) ;
        }
        else{
            return String.format("select %s;banana %d %d", wormID, x, y ) ;
        }
    }
}