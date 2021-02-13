package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;

import java.util.*;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
    }

    public Command run(){
        Position P = new Position();
        P.x = 16;
        P.y = 16;

        Position P1;
        P1 = getNextCellShortestPath(P);
        if(this.gameState.map[P1.y][P1.x].type == CellType.DIRT){
            return new DigCommand(P1.x, P1.y);
        }
        else{
            return new MoveCommand(P1.x, P1.y);
        }
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    private int getLinearDistance(Position P1, Position P2){
        int count = 0;
        if(P1.y == P2.y && P1.x == P2.x){
            return count;
        }
        else if(P1.y == P2.y){
            int val = Math.abs(P1.x - P2.x);
            for(int i = 1; i <= val; i++){
                int a;
                if(P1.x > P2.x){
                    a = -1*i;
                }
                else{
                    a = i;
                }
                if(gameState.map[P1.y][P1.x + a].type == CellType.DIRT){
                    count += 2;
                }
                else{
                    count += 1;
                }
            }
        }
        else if(P1.x == P2.x){
            int val = Math.abs(P1.y - P2.y);
            for(int i = 1; i <= val; i++){
                int b;
                if(P1.y > P2.y){
                    b = i*-1;
                }
                else{
                    b = i;
                }
                if(gameState.map[P1.y + b][P1.x].type == CellType.DIRT){
                    count += 2;
                }
                else{
                    count += 1;
                }
            }
        }
        else if(Math.abs(P1.x - P2.x) == Math.abs(P1.y - P2.y)){
            int val = Math.abs(P1.y - P2.y);
            for(int i = 1; i <= val; i++){
                int a,b;
                if(P1.x > P2.x){
                    a = -1*i;
                }
                else{
                    a = i;
                }
                if(P1.y > P2.y){
                    b = i*-1;
                }
                else{
                    b = i;
                }
                if(gameState.map[P1.y + b][P1.x + a].type == CellType.DIRT){
                    count += 2;
                }
                else{
                    count += 1;
                }
            }
        }
        else{
            return 999;
        }
        return count;
    }

    private Position getNextCellShortestPath(Position P){
        Map<Position, Integer> map = new HashMap<Position, Integer>();
        for(int i = currentWorm.position.x - 1; i <= currentWorm.position.x + 1; i++){
            for(int j = currentWorm.position.y - 1; j <= currentWorm.position.y + 1; j++){
                Position P1 = new Position();
                P1.x = i;
                P1.y = j;

                if(!(P1.x == currentWorm.position.x && P1.y == currentWorm.position.y) && isCoordinateValid(P1)){
                    map.put(P1, getShortestDistance(getVertexForDijkstra(P1,P)));
                }
            }
        }
        int minDistance = Collections.min(map.values());
        Position Ps = new Position();
        for(Map.Entry<Position, Integer> m : map.entrySet()){
            if(m.getValue() == minDistance){
                Ps = m.getKey();
                break;
            }
        }

        return Ps;
    }

    private boolean isDiagonal(Position Psrc, Position Pdest){
        return Math.abs(Psrc.x - Pdest.x) == Math.abs(Psrc.y - Pdest.y);
    }

    private boolean isSejajarSbX(Position Psrc, Position Pdest){
        return Psrc.y == Pdest.y;
    }

    private boolean isSejajarSbY(Position Psrc, Position Pdest){
        return Psrc.x == Pdest.x;
    }

    private boolean isCoordinateValid(Position P){
        return ((P.x >= 0 && P.x <= 32)&&(P.y >= 0 && P.y <= 32)&&(gameState.map[P.y][P.x].type != CellType.DEEP_SPACE));
    }

    private List<Cell> getVertexForDijkstra(Position Psrc, Position Pdest){
        List<Cell> L = new ArrayList<Cell>();
        L.add(gameState.map[Psrc.y][Psrc.x]);
        if(!isDiagonal(Psrc, Pdest)) {
            if (isSejajarSbX(Psrc, Pdest)) {
                if (Math.abs(Psrc.x - Pdest.x) % 2 == 0) {
                    int val = (Pdest.x - Psrc.x) / 2;
                    L.add(gameState.map[Pdest.y + val][Pdest.x - val]);
                    L.add(gameState.map[Pdest.y - val][Pdest.x - val]);
                }
            } else if (isSejajarSbY(Psrc, Pdest)) {
                if (Math.abs(Psrc.y - Pdest.y) % 2 == 0) {
                    int val = (Pdest.y - Psrc.y) / 2;
                    L.add(gameState.map[Pdest.y - val][Pdest.x + val]);
                    L.add(gameState.map[Pdest.y - val][Pdest.x - val]);
                }
            } else {
                Position P1 = new Position();
                Position P2 = new Position();
                P1.x = Psrc.x;
                P1.y = Psrc.y;;
                P2.x = Pdest.x;
                P2.y = Pdest.y;
                if (Math.abs(Psrc.y - Pdest.y) < Math.abs(Psrc.x - Pdest.x)) {
                    for (int i = 1; i <= Math.abs(Psrc.x - Pdest.x); i++) {
                        if (P1.x < Pdest.x) {
                            P1.x++;
                        } else {
                            P1.x--;
                        }
                        if (Math.abs(P1.x - Pdest.x) == Math.abs(P1.y - Pdest.y)) {
                            if (isCoordinateValid(P1)) {
                                L.add(gameState.map[P1.y][P1.x]);
                            }
//                        if(Math.abs(currentWorm.position.x - P1.x) % 2 == 0){
//                            int val = (P1.x - currentWorm.position.x) / 2;
//                            L.add(gameState.map[P1.y + val][P1.x - val]);
//                            L.add(gameState.map[P1.y - val][P1.x - val]);
//                        }
                            break;
                        }
                    }
                    for (int i = 1; i <= Math.abs(Pdest.x - Psrc.x); i++) {
                        if (P2.x < Psrc.x) {
                            P2.x++;
                        } else {
                            P2.x--;
                        }
                        if (Math.abs(P2.x - Psrc.x) == Math.abs(P2.y - Psrc.y)) {
                            if (isCoordinateValid(P2)) {
                                L.add(gameState.map[P2.y][P2.x]);
                            }
//                        if(Math.abs(P.x - P2.x) % 2 == 0){
//                            int val = (P2.x - P.x) / 2;
//                            L.add(gameState.map[P2.y + val][P2.x - val]);
//                            L.add(gameState.map[P2.y - val][P2.x - val]);
//                        }
                            break;
                        }
                    }
                } else {
                    for (int i = 1; i <= Math.abs(Psrc.y - Pdest.y); i++) {
                        if (P1.y < Pdest.y) {
                            P1.y++;
                        } else {
                            P1.y--;
                        }
                        if (Math.abs(P1.x - Pdest.x) == Math.abs(P1.y - Pdest.y)) {
                            if (isCoordinateValid(P1)) {
                                L.add(gameState.map[P1.y][P1.x]);
                            }
//                        if(Math.abs(currentWorm.position.y - P1.y) % 2 == 0){
//                            int val = (P1.y - currentWorm.position.y) / 2;
//                            L.add(gameState.map[P1.y - val][P1.x + val]);
//                            L.add(gameState.map[P1.y - val][P1.x - val]);
//                        }
                            break;
                        }
                    }
                    for (int i = 1; i <= Math.abs(Pdest.y - Psrc.y); i++) {
                        if (P2.y < Psrc.y) {
                            P2.y++;
                        } else {
                            P2.y--;
                        }
                        if (Math.abs(P2.x - Psrc.x) == Math.abs(P2.y - Psrc.y)) {
                            if (isCoordinateValid(P2)) {
                                L.add(gameState.map[P2.y][P2.x]);
                            }
//                        if(Math.abs(P.y - P2.y) % 2 == 0){
//                            int val = (P2.y - P.y) / 2;
//                            L.add(gameState.map[P2.y - val][P2.x + val]);
//                            L.add(gameState.map[P2.y - val][P2.x - val]);
//                        }
                            break;
                        }
                    }
                }
            }
        }
        L.add(gameState.map[Pdest.y][Pdest.x]);
        return L;
    }

    private int dijkstraSelection(int[] L, int n, List<Integer> S){
        int u = 0;
        int min = 999;
        for(int i = 1; i < L.length; i++){
            if(L[i] < min && !S.contains(min)){
                min = L[i];
                u = i;
            }
        }
        return u;
    }

    private int getShortestDistance(List<Cell> C){
        int[][] graph = new int[C.size()][C.size()];
        for(int i = 0; i < C.size(); i++){
            for(int j = 0; j < C.size(); j++){
                Position p1 = new Position();
                Position p2 = new Position();
                p1.x = C.get(i).x;
                p1.y = C.get(i).y;
                p2.x = C.get(j).x;
                p2.y = C.get(j).y;
                graph[i][j] = getLinearDistance(p1, p2);
            }
        }

        for(int i = 0; i < C.size(); i++){
            System.out.println(C.get(i).x + "," + C.get(i).y);
        }
        System.out.println();

        //Dijkstra Shortest Path

        int[] L = new int[C.size()];
        L[0] = 0;
        for(int i = 1; i < L.length; i++){
            L[i] = 999;
        }
        List<Integer> S = new ArrayList<Integer>();

        for(int i = 0; i < L.length; i++){
            int u = dijkstraSelection(L, i, S);
            S.add(u);
            for(int j = u; j < graph[u].length; j++){
                if(graph[u][j] != 999 && (L[u] + graph[u][j]) < L[j]){
                    L[j] = L[u] + graph[u][j];
                }
            }
        }
        return L[L.length-1];
    }









//    public Command run() {
//
//        Worm enemyWorm = getFirstWormInRange();
//        if (enemyWorm != null) {
//            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
//            return new ShootCommand(direction);
//        }
//
//        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
//        int cellIdx = random.nextInt(surroundingBlocks.size());
//
//        Cell block = surroundingBlocks.get(cellIdx);
//        if (block.type == CellType.AIR) {
//            return new MoveCommand(block.x, block.y);
//        } else if (block.type == CellType.DIRT) {
//            return new DigCommand(block.x, block.y);
//        }
//
//        return new DoNothingCommand();
//    }
//
//    private Worm getFirstWormInRange() {
//
//        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
//                .stream()
//                .flatMap(Collection::stream)
//                .map(cell -> String.format("%d_%d", cell.x, cell.y))
//                .collect(Collectors.toSet());
//
//        for (Worm enemyWorm : opponent.worms) {
//            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
//            if (cells.contains(enemyPosition)) {
//                return enemyWorm;
//            }
//        }
//
//        return null;
//    }
//
//    private List<List<Cell>> constructFireDirectionLines(int range) {
//        List<List<Cell>> directionLines = new ArrayList<>();
//        for (Direction direction : Direction.values()) {
//            List<Cell> directionLine = new ArrayList<>();
//            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {
//
//                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
//                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);
//
//                if (!isValidCoordinate(coordinateX, coordinateY)) {
//                    break;
//                }
//
//                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
//                    break;
//                }
//
//                Cell cell = gameState.map[coordinateY][coordinateX];
//                if (cell.type != CellType.AIR) {
//                    break;
//                }
//
//                directionLine.add(cell);
//            }
//            directionLines.add(directionLine);
//        }
//
//        return directionLines;
//    }
//
//    private List<Cell> getSurroundingCells(int x, int y) {
//        ArrayList<Cell> cells = new ArrayList<>();
//        for (int i = x - 1; i <= x + 1; i++) {
//            for (int j = y - 1; j <= y + 1; j++) {
//                // Don't include the current position
//                if (i != x && j != y && isValidCoordinate(i, j)) {
//                    cells.add(gameState.map[j][i]);
//                }
//            }
//        }
//
//        return cells;
//    }
//
//    private int euclideanDistance(int aX, int aY, int bX, int bY) {
//        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
//    }
//
//    private boolean isValidCoordinate(int x, int y) {
//        return x >= 0 && x < gameState.mapSize
//                && y >= 0 && y < gameState.mapSize;
//    }
//
//    private Direction resolveDirection(Position a, Position b) {
//        StringBuilder builder = new StringBuilder();
//
//        int verticalComponent = b.y - a.y;
//        int horizontalComponent = b.x - a.x;
//
//        if (verticalComponent < 0) {
//            builder.append('N');
//        } else if (verticalComponent > 0) {
//            builder.append('S');
//        }
//
//        if (horizontalComponent < 0) {
//            builder.append('W');
//        } else if (horizontalComponent > 0) {
//            builder.append('E');
//        }
//
//        return Direction.valueOf(builder.toString());
//    }
}
