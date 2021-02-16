package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;
import za.co.entelect.challenge.enums.PowerUpType;

import java.sql.SQLOutput;
import java.util.*;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;
    private MyWorm[] allMyWorms;
    private Worm[] allOpponentWorms;
    private Cell[] presummedPowerupCells;


    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
        this.allMyWorms = gameState.myPlayer.worms;
        this.allOpponentWorms = gameState.opponents[0].worms;
        this.presummedPowerupCells = getAllCellsWithPowerup();
    }

    public Command run(){
        Position P = new Position();
        P.x = 16;
        P.y = 16;

        for(int i = 0; i < 3; i++){
            if(isEnemyShootable(allOpponentWorms[i])){
                Direction direction = resolveDirection(currentWorm.position, allOpponentWorms[i].position);
                return new ShootCommand(direction);
            }
        }

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

    private Position[] getEnemyPosition () {
        Position[] EnemyPosition = new Position[3];
        for (int k = 0; k < 3; k++) {
            if (gameState.opponents[0].worms[k].health > 0) {
                EnemyPosition[k] = gameState.opponents[0].worms[k].position;
            }
            else {
                Position P = new Position();
                P.x = -1; P.y = -1;
                EnemyPosition[k] = P;
            }
        }
        return EnemyPosition;
    }

    private int getEuclideanDistance (Position P1, Position P2) {
        double s = Math.pow((P1.x - P2.x), 2) + Math.pow((P1.y - P2.y), 2);
        return (int) Math.round(Math.sqrt(s));
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
                    int count = 0;
                    if(gameState.map[P1.y][P1.x].type == CellType.DIRT){
                        count += 2;
                    }
                    else{
                        count += 1;
                    }
                    map.put(P1, getShortestDistance(getVertexForDijkstra(P1,P)) + count);
//                    System.out.println(getShortestDistance(getVertexForDijkstra(P1,P)) + count);
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


    /* Predikat Methods */
    private boolean isWormAlive (int playerId, int wormId) {
        // playerId: 1 (player), 2 (opponent)
        // wormId: 1 (Commando), 2 (Agent), 3 (Technologist)
        if (playerId == 1) {
            return gameState.myPlayer.worms[wormId-1].health > 0;
        }
        else {
            return gameState.opponents[0].worms[wormId-1].health > 0;
        }
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

    private boolean isCoordinateXYValid(int x, int y) {
        return (x >= 0 && x <= 32)&&(y >= 0 && y <= 32)&&(gameState.map[y][x].type != CellType.DEEP_SPACE);
    }

    private boolean[] isEnemyInRangeBananaBomb (Position PAgent) {
        // cek apakah ada Opponent's worms yang dapat dikenai Banana Bomb oleh Agent
        /* Prekondisi:
            PAgent valid (masih hidup), masih ada Banana Bomb di inventory
         */
        int xAgent = PAgent.x;
        int yAgent = PAgent.y;
        boolean[] isBombable = {FALSE, FALSE, FALSE};

        Position[] enemyPosition = getEnemyPosition();

        int[] euclideanDistances = new int[3];
        for (int k = 0; k < 3; k++) {
            if (isWormAlive(2, k+1)) {
                euclideanDistances[k] = getEuclideanDistance(PAgent, enemyPosition[k]);
            }
            else {
                euclideanDistances[k] = 999;
            }
        }

        for (int k = 0; k < 3; k++) {
            if (euclideanDistances[k] > 7) {
                isBombable[k] = FALSE;
                continue;
            }
            for (int i = -2; i < 3; i++) {
                for (int j = -2; j < 3; j++) {
                    Position Ptemp = new Position();
                    Ptemp.x = enemyPosition[k].x + i;
                    Ptemp.y = enemyPosition[k].y + i;

                    if (isCellInBananaBombBlastRange(enemyPosition[k], Ptemp)) {
                        int throwDistance = getEuclideanDistance(Ptemp, PAgent);
                        if (throwDistance <= 5) {
                            isBombable[k] = TRUE;
                        }
                    }
                }
            }
        }

        return isBombable;
    }

    private boolean isCellInBananaBombBlastRange(Position PBomb, Position PTarget) {
        int xDifference = Math.abs(PBomb.x - PTarget.x);
        int yDifference = Math.abs(PBomb.y - PTarget.y);
        if (xDifference <= 1 && yDifference <= 1) {
            return TRUE;
        }
        else if (xDifference == 0 && yDifference == 2) {
            return TRUE;
        }
        else if (xDifference == 2 && yDifference == 0) {
            return TRUE;
        }
        return FALSE;
    }

    private boolean isCommandoAlive (GameState GS) {
        return (gameState.myPlayer.worms[0].id > 0);
    }
    private boolean isAgentAlive (GameState GS) {
        return (gameState.myPlayer.worms[1].id > 0);
    }
    private boolean isTechnologistAlive (GameState GS) {
        return (gameState.myPlayer.worms[2].id > 0);
    }
    //private boolean isBananaBombAvailable (GameState GS) {
        //return (gameState.myPlayer.worms[1].weapon.);
    //}

    private List<Cell> getVertexForDijkstra(Position Psrc, Position Pdest){
        List<Cell> L = new ArrayList<Cell>();
        L.add(gameState.map[Psrc.y][Psrc.x]);
        if(!isDiagonal(Psrc, Pdest)) {
            if (isSejajarSbX(Psrc, Pdest)) {
//                if (Math.abs(Psrc.x - Pdest.x) % 2 == 0) {
//                    int val = (Pdest.x - Psrc.x) / 2;
//                    L.add(gameState.map[Pdest.y + val][Pdest.x - val]);
//                    L.add(gameState.map[Pdest.y - val][Pdest.x - val]);
//                }
            } else if (isSejajarSbY(Psrc, Pdest)) {
//                if (Math.abs(Psrc.y - Pdest.y) % 2 == 0) {
//                    int val = (Pdest.y - Psrc.y) / 2;
//                    L.add(gameState.map[Pdest.y - val][Pdest.x + val]);
//                    L.add(gameState.map[Pdest.y - val][Pdest.x - val]);
//                }
            } else {
                Position P1 = new Position();
                Position P2 = new Position();
                P1.x = Psrc.x;
                P1.y = Psrc.y;
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
//        for(int i = 0; i < C.size(); i++){
//            System.out.print(i + " ");
//            for(int j = 0; j < C.size(); j++){
//                System.out.print(graph[i][j] + " ");
//            }
//            System.out.println();
//        }
//        System.out.println();
//        for(int i = 0; i < C.size(); i++){
//            System.out.println(C.get(i).x + "," + C.get(i).y);
//        }
//        System.out.println();

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

    public int getClosestSumOfDistanceBetweenWorm() {
        // Untuk Worm yang sudah mati, healthnya <= 0
        // Jika hanya tersisa satu worm, maka return 0
        int result = 0;
        MyWorm W1 = allMyWorms[0];
        MyWorm W2 = allMyWorms[1];
        MyWorm W3 = allMyWorms[2];
        if(W1.health > 0 && W2.health > 0 && W3.health > 0) {
            int P12 = getLinearDistance(W1.position, W2.position);
            int P13 = getLinearDistance(W1.position, W3.position);
            int P23 = getLinearDistance(W2.position, W3.position);
            if(P12 < P13 && P12 < P23){
                result = P12;
            } else if (P13 < P12 && P13 < P23) {
                result = P13;
            } else {
                result = P23;
            }
        } else if (W1.health <= 0 && W2.health > 0 && W3.health > 0) {
            result = getLinearDistance(W2.position, W3.position);
        } else if (W1.health > 0 && W2.health <= 0 && W3.health > 0) {
            result = getLinearDistance(W1.position, W3.position);
        } else if (W1.health > 0 && W2.health > 0 && W3.health <= 0) {
            result = getLinearDistance(W1.position, W2.position);
        } else {
            result = 0;
        }
        return result;
    }

    public boolean isEnemyShootable(Worm targetWorm) {
        // P1 = currentWorm Position
        // P2 = targetWorm Position

        boolean isShootable = false;
        int maxRange = 4;
        Position P1 = currentWorm.position;
        Position P2 = targetWorm.position;
        if(getLinearDistance(P1,P2) <= maxRange && targetWorm.health > 0 && !isAnyObstacleBetween(P1,P2)) {
            isShootable = true;
        }
//        if(P2.y == P1.y && getLinearDistance(P1,P2) <= maxRange && targetWorm.health > 0) {
//            if(isAnyObstacleInRange(P1,P2,0) || isAnyWormInRange(P1,P2,0)){
//                isShootable = false;
//            }
//        } else if (P2.x == P1.x && getLinearDistance(P1,P2) <= maxRange && targetWorm.health > 0) {
//            if(isAnyObstacleInRange(P1,P2,1) || isAnyWormInRange(P1,P2,1)){
//                isShootable = false;
//            }
//        } else if (P2.x + P2.y == P1.x + P1.y && getLinearDistance(P1,P2) <= maxRange && targetWorm.health > 0) {
//            if(isAnyObstacleInRange(P1,P2,2) || isAnyWormInRange(P1,P2,2)){
//                isShootable = false;
//            }
//        } else if (P1.x - P2.x == P1.y - P2.y && getLinearDistance(P1,P2) <= maxRange && targetWorm.health > 0) {
//            if(isAnyObstacleInRange(P1,P2,3) || isAnyWormInRange(P1,P2,3)){
//                isShootable = false;
//            }
//        } else {
//            isShootable = false;
//        }
        return isShootable;
    }

    private boolean isAnyMyWormInPosition(int x, int y){
        Position P = new Position();
        P.x = x;
        P.y = y;
        for(int i = 0; i <= 3; i++){
            if(allMyWorms[i] != currentWorm && allMyWorms[i].position == P){
                return true;
            }
        }
        return false;
    }

    private boolean isAnyObstacleBetween(Position P1, Position P2){
        boolean obstacle = false;
        if(P1.y == P2.y){
            int val = Math.abs(P1.x - P2.x);
            for(int i = 1; i <= val; i++){
                int a;
                if(P1.x > P2.x){
                    a = -1*i;
                }
                else{
                    a = i;
                }
                if(gameState.map[P1.y][P1.x + a].type == CellType.DIRT && isAnyMyWormInPosition(P1.x + a,P1.y)){
                    obstacle = true;
                    break;
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
                if(gameState.map[P1.y + b][P1.x].type == CellType.DIRT && isAnyMyWormInPosition(P1.x,P1.y + b)){
                    obstacle = true;
                    break;
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
                if(gameState.map[P1.y + b][P1.x + a].type == CellType.DIRT && isAnyMyWormInPosition(P1.x + a,P1.y + b)){
                    obstacle = true;
                    break;
                }
            }
        }
        return obstacle;
    }

    public boolean isAnyObstacleInRange(Position P1, Position P2, int directionSearch) {
        // Asumsi input valid
        // Untuk search horizontal (P1.y == P2.y), directionSearch = 0
        // Untuk search vertikal (P1.x == P2.x), directionSearch = 1
        // Untuk search diagonal dari kiri atas ke kanan bawah atau sebaliknya, directionSearch = 2
        // Untuk search diagonal dari kiri bawah ke kanan atas atau sebaliknya, directionSearch = 3
        boolean obstacleInRange = false;
        if(directionSearch == 0) {
            if(P1.x < P2.x) {
                int i = P1.x + 1;
                while(i < P2.x && !obstacleInRange){
                    if(gameState.map[i][P1.y].type == CellType.DEEP_SPACE || gameState.map[i][P1.y].type == CellType.DIRT) {
                        obstacleInRange = true;
                    }
                    i = i + 1;
                }
            } else {
                int i = P2.x + 1;
                while(i < P1.x && !obstacleInRange){
                    if(gameState.map[i][P1.y].type == CellType.DEEP_SPACE || gameState.map[i][P1.y].type == CellType.DIRT) {
                        obstacleInRange = true;
                    }
                    i = i + 1;
                }
            }
        } else if (directionSearch == 1) {
            if(P1.y < P2.y) {
                int i = P1.y + 1;
                while(i < P2.y && !obstacleInRange){
                    if(gameState.map[P1.x][i].type == CellType.DEEP_SPACE || gameState.map[P1.x][i].type == CellType.DIRT) {
                        obstacleInRange = true;
                    }
                    i = i + 1;
                }
            } else {
                int i = P2.y + 1;
                while(i < P1.y && !obstacleInRange){
                    if(gameState.map[P1.x][i].type == CellType.DEEP_SPACE || gameState.map[P1.x][i].type == CellType.DIRT) {
                        obstacleInRange = true;
                    }
                    i = i + 1;
                }
            }
        } else if (directionSearch == 2) {
            Position P3 = new Position();
            if(P1.x < P2.x) {
                P3.x = P1.x + 1;
                P3.y = P1.y - 1;
                while(!obstacleInRange && P3.x != P2.x && P3.y != P2.y) {
                    if(gameState.map[P3.x][P3.y].type == CellType.DEEP_SPACE || gameState.map[P3.x][P3.y].type == (CellType.DIRT)) {
                        obstacleInRange = true;
                    }
                    P3.x = P3.x + 1;
                    P3.y = P3.y - 1;
                }
            } else {
                P3.x = P2.x + 1;
                P3.y = P2.y - 1;
                while(!obstacleInRange && P3.x != P1.x && P3.y != P1.y) {
                    if(gameState.map[P3.x][P3.y].type == CellType.DEEP_SPACE || gameState.map[P3.x][P3.y].type == (CellType.DIRT)) {
                        obstacleInRange = true;
                    }
                    P3.x = P3.x + 1;
                    P3.y = P3.y - 1;
                }
            }
        } else {
            Position P3 = new Position();
            if(P1.x < P2.x) {
                P3.x = P1.x + 1;
                P3.y = P1.y + 1;
                while(!obstacleInRange && P3.x != P2.x && P3.y != P2.y) {
                    if(gameState.map[P3.x][P3.y].type == CellType.DEEP_SPACE || gameState.map[P3.x][P3.y].type == CellType.DIRT) {
                        obstacleInRange = true;
                    }
                    P3.x = P3.x + 1;
                    P3.y = P3.y + 1;
                }
            } else {
                P3.x = P2.x + 1;
                P3.y = P2.y + 1;
                while(!obstacleInRange && P3.x != P1.x && P3.y != P1.y) {
                    if(gameState.map[P3.x][P3.y].type == CellType.DEEP_SPACE || gameState.map[P3.x][P3.y].type == CellType.DIRT) {
                        obstacleInRange = true;
                    }
                    P3.x = P3.x + 1;
                    P3.y = P3.y + 1;
                }
            }
        }
        return obstacleInRange;
    }

    public boolean isAnyWormInRange(Position P1, Position P2, int directionSearch) {
        // Asumsi input valid
        // Untuk search horizontal (P1.y == P2.y), directionSearch = 0
        // Untuk search vertikal (P1.x == P2.x), directionSearch = 1
        // Untuk search diagonal dari kiri atas ke kanan bawah atau sebaliknya, directionSearch = 2
        // Untuk search diagonal dari kiri bawah ke kanan atas atau sebaliknya, directionSearch = 3
        boolean wormInRange = false;
        if(directionSearch == 0) {
            if(P1.x < P2.x) {
                int i = P1.x + 1;
                while(i < P2.x && !wormInRange){
                    int j = 0;
                    while(!wormInRange && j < 3){
                        if(allMyWorms[j].position.x == i && allMyWorms[j].position.y == P1.y || allOpponentWorms[j].position.x == i && allMyWorms[j].position.y == P1.y) {
                            wormInRange = true;
                        }
                        j = j + 1;
                    }
                    i = i + 1;
                }
            } else {
                int i = P2.x + 1;
                while(i < P1.x && !wormInRange){
                    int j = 0;
                    while(!wormInRange && j < 3){
                        if(allMyWorms[j].position.x == i && allMyWorms[j].position.y == P1.y || allOpponentWorms[j].position.x == i && allMyWorms[j].position.y == P1.y) {
                            wormInRange = true;
                        }
                        j = j + 1;
                    }
                    i = i + 1;
                }
            }
        } else if (directionSearch == 1) {
            if(P1.y < P2.y) {
                int i = P1.y + 1;
                while(i < P2.y && !wormInRange){
                    int j = 0;
                    while(!wormInRange && j < 3){
                        if(allMyWorms[j].position.y == i && allMyWorms[j].position.x == P1.x || allOpponentWorms[j].position.y == i && allMyWorms[j].position.x == P1.x) {
                            wormInRange = true;
                        }
                        j = j + 1;
                    }
                    i = i + 1;
                }
            } else {
                int i = P2.y + 1;
                while(i < P1.y && !wormInRange){
                    int j = 0;
                    while(!wormInRange && j < 3){
                        if(allMyWorms[j].position.y == i && allMyWorms[j].position.x == P1.x || allOpponentWorms[j].position.y == i && allMyWorms[j].position.x == P1.x) {
                            wormInRange = true;
                        }
                        j = j + 1;
                    }
                    i = i + 1;
                }
            }
        } else if (directionSearch == 2) {
            Position P3 = new Position();
            if(P1.x < P2.x) {
                P3.x = P1.x + 1;
                P3.y = P1.y - 1;
                while(!wormInRange && P3.x != P2.x && P3.y != P2.y) {
                    int j = 0;
                    while(!wormInRange && j < 3) {
                        if(allMyWorms[j].position.x == P3.x && allMyWorms[j].position.y == P3.y || allOpponentWorms[j].position.x == P3.x && allOpponentWorms[j].position.y == P3.y) {
                            wormInRange = true;
                        }
                        j = j + 1;
                    }
                    P3.x = P3.x + 1;
                    P3.y = P3.y - 1;
                }
            } else {
                P3.x = P2.x + 1;
                P3.y = P2.y - 1;
                while(!wormInRange && P3.x != P1.x && P3.y != P1.y) {
                    int j = 0;
                    while(!wormInRange && j < 3) {
                        if(allMyWorms[j].position.x == P3.x && allMyWorms[j].position.y == P3.y || allOpponentWorms[j].position.x == P3.x && allOpponentWorms[j].position.y == P3.y) {
                            wormInRange = true;
                        }
                        j = j + 1;
                    }
                    P3.x = P3.x + 1;
                    P3.y = P3.y - 1;
                }
            }
        } else {
            Position P3 = new Position();
            if(P1.x < P2.x) {
                P3.x = P1.x + 1;
                P3.y = P1.y + 1;
                while(!wormInRange && P3.x != P2.x && P3.y != P2.y) {
                    int j = 0;
                    while(!wormInRange && j < 3) {
                        if(allMyWorms[j].position.x == P3.x && allMyWorms[j].position.y == P3.y || allOpponentWorms[j].position.x == P3.x && allOpponentWorms[j].position.y == P3.y) {
                            wormInRange = true;
                        }
                        j = j + 1;
                    }
                    P3.x = P3.x + 1;
                    P3.y = P3.y + 1;
                }
            } else {
                P3.x = P2.x + 1;
                P3.y = P2.y + 1;
                while(!wormInRange && P3.x != P1.x && P3.y != P1.y) {
                    int j = 0;
                    while(!wormInRange && j < 3) {
                        if(allMyWorms[j].position.x == P3.x && allMyWorms[j].position.y == P3.y || allOpponentWorms[j].position.x == P3.x && allOpponentWorms[j].position.y == P3.y) {
                            wormInRange = true;
                        }
                        j = j + 1;
                    }
                    P3.x = P3.x + 1;
                    P3.y = P3.y - 1;
                }
            }
        }
        return wormInRange;
    }

    public Position getClosestEnemy() {
        int closestDistance = 9999;
        Position P3 = new Position();
        P3.x = -99;
        P3.y = -99;
        for(int i=0;i<allMyWorms.length;i++) {
            for(int j=0;j<allOpponentWorms.length;j++) {
                if(getLinearDistance(allMyWorms[i].position, allOpponentWorms[j].position) < closestDistance) {
                    closestDistance = getLinearDistance(allMyWorms[i].position, allOpponentWorms[j].position);
                    P3.x = allOpponentWorms[j].position.x;
                    P3.y = allOpponentWorms[j].position.y;
                }
            }
        }
        return P3;
    }

    public Cell[] getAllCellsWithPowerup() {
        // Dijalankan hanya pada saat ronde paling awal
        // Asumsikan jumlah powerup <= 20
        Cell[] powerupCells = new Cell[20];
        int counter = 0;
        for(int i=0;i<gameState.map.length;i++) {
            for(int j=0;j<gameState.map[i].length;j++) {
                if(gameState.map[i][j].powerUp != null) {
                    powerupCells[counter] = gameState.map[i][j];
                    counter = counter + 1;
                }
            }
        }
        return powerupCells;
    }

    public Position getClosestPowerup() {
        Position P3 = new Position();
        Position tempPowerupPosition = new Position();
        P3.x = 9999;
        P3.y = 9999;
        int minDistance = 9999;
        for(int i=0;i<allMyWorms.length;i++) {
            for(int j=0;j<presummedPowerupCells.length;j++) {
                if(presummedPowerupCells[j] == null) {
                    break;
                } else {
                    tempPowerupPosition.x = presummedPowerupCells[j].x;
                    tempPowerupPosition.y = presummedPowerupCells[j].y;
                    if (presummedPowerupCells[j].powerUp != null && getLinearDistance(allMyWorms[i].position, tempPowerupPosition) < minDistance) {
                        minDistance = getLinearDistance(allMyWorms[i].position, tempPowerupPosition);
                        P3 = tempPowerupPosition;
                    }
                }
            }
        }
        return P3;
    }

    private int getCurrentWormDistanceFromLava(){
        ArrayList<Cell> L = new ArrayList<Cell>();
        for(int i = 0; i < gameState.mapSize; i++){
            for(int j = 0; j < gameState.mapSize; j++){
                if(gameState.map[i][j].type == CellType.LAVA){
                    L.add(gameState.map[i][j]);
                }
            }
        }
        int minDistance = 9999;
        if(!L.isEmpty()){
            for(int i = 0; i < L.size(); i++){
                Position lavaCell = new Position();
                lavaCell.x = L.get(i).x;
                lavaCell.y = L.get(i).y;
                int minL = getEuclideanDistance(currentWorm.position, lavaCell);
                if(minL < minDistance){
                    minDistance = minL;
                }
            }
        }
        return minDistance;
    }

    private Worm getEnemyWithLowestHealth(){
        int minHealth = 9999;
        Worm enemy = new Worm();
        for(int i = 0; i <3; i++){
            if(allOpponentWorms[i].health < minHealth && allOpponentWorms[i].health != 0){
                minHealth = allOpponentWorms[i].health;
                enemy = allOpponentWorms[i];
            }
        }
        return enemy;
    }

    private int getCurrentWormHealth(){
        return currentWorm.health;
    }

    private int getClosestEnemyCurrentHealth() {
        Position P = new Position();
        P = getClosestEnemy();
        int health = 0;
        for(int i = 0; i < 3; i++){
            if(allOpponentWorms[i].position == P){
                health = allMyWorms[i].health;
                break;
            }
        }
        return health;
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
    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }
}