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

    // akan menyimpan posisi terbaik untuk
    private List<Position> candidateBananaBombPositionByPlayer;
    private List<Position> candidateBananaBombPositionByEnemy;
    private List<Position> candidateSnowballPositionByPlayer;
    private List<Position> candidateSnowballPositionByEnemy;
    private int[] impactOfBeingFrozen;


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
        Position P = getClosestEnemy();
        for(int i = 0; i < 3; i++) {
            if (allOpponentWorms[i].roundsUntilUnfrozen > 0 && gameState.myPlayer.remainingWormSelections > 0) {
                for (int j = 0; j < 3; j++) {
                    Direction direction = resolveDirection(allMyWorms[j].position, allOpponentWorms[i].position);
                    if (isEnemyShootable(allMyWorms[j], allOpponentWorms[i])) {
                        return new SelectCommand(allMyWorms[j].id, "shoot", direction);
                    }
                }
            }
        }
        if(allMyWorms[2].snowballs.count == 0 && gameState.myPlayer.remainingWormSelections > 0) {
            for(int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    Direction direction = resolveDirection(allMyWorms[j].position, allOpponentWorms[i].position);
                    if (isEnemyShootable(allMyWorms[j], allOpponentWorms[i])) {
                        return new SelectCommand(allMyWorms[j].id, "shoot", direction);
                    }
                }
            }
        }
        weaponChoiceAndPosition W = greedyByWeaponChoice();
        if(W.weapon == 1){
            Direction direction = resolveDirection(currentWorm.position, W.position);
            return new ShootCommand(direction);
        }
        else if(W.weapon == 2){
            return new BananaBombCommand(W.position.x,W.position.y);
        }
        else if(W.weapon == 3){
            return new SnowballCommand(W.position.x,W.position.y);
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
        // mendapatkan Position worms musuh, (-999,-999) jika worms mati
        Position[] EnemyPosition = new Position[3];
        for (int k = 0; k < 3; k++) {
            if (gameState.opponents[0].worms[k].health > 0) {
                EnemyPosition[k] = gameState.opponents[0].worms[k].position;
            }
            else {
                Position P = new Position();
                P.x = -999; P.y = -999;
                EnemyPosition[k] = P;
            }
        }
        return EnemyPosition;
    }

    private Position[] getMyWormsPosition () {
        // mendapatkan Position worms Player, (-999,-999) jika worms mati
        Position[] myWormsPosition = new Position[3];
        for (int k = 0; k < 3; k++) {
            if (gameState.myPlayer.worms[k].health > 0) {
                myWormsPosition[k] = gameState.myPlayer.worms[k].position;
            }
            else {
                myWormsPosition[k] = createPosition(-999,-999);
            }
        }
        return myWormsPosition;
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
                if(!(P1.x == currentWorm.position.x && P1.y == currentWorm.position.y) && isCoordinateValid(P1) && !isAnyMyWormInPosition(P1.x,P1.y)){
                    int count = 0;
                    if(gameState.map[P1.y][P1.x].type == CellType.DIRT){
                        count += 2;
                    }
                    else{
                        count += 1;
                    }
                    map.put(P1, getShortestDistance(getVertexForDijkstra(P1,P)) + count);
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

    private boolean isEnemyBananaBombable(Worm targetWorm){
        int distance = getEuclideanDistance(currentWorm.position, targetWorm.position);
        return distance == 5;
    }

    private boolean isMyWormBananaBombable(Position banana){
        for(int i = 0; i < 3; i++){
            if(getEuclideanDistance(allMyWorms[i].position,banana) <= 2){
                return true;
            }
        }
        return false;
    }

    private boolean isEnemySnowballable(Worm targetWorm){
        int distance = getEuclideanDistance(currentWorm.position, targetWorm.position);
        return distance == 5;
    }

    private boolean isMyWormSnowballable(Position snow){
        for(int i = 0; i < 3; i++){
            if(getEuclideanDistance(allMyWorms[i].position,snow) <= 1){
                return true;
            }
        }
        return false;
    }

    public class weaponChoiceAndPosition{
        public int weapon;
        public Position position;
        public weaponChoiceAndPosition(int weapon, Position position){
            this.weapon = weapon;
            this.position = position;
        }
        public weaponChoiceAndPosition() {

        }
    }

    private weaponChoiceAndPosition greedyByWeaponChoice(){
        ArrayList<Worm> enemyWorm = new ArrayList<Worm>();
        for(int i = 0; i < 3; i++){
            enemyWorm.add(allOpponentWorms[i]);
        }
        Collections.sort(enemyWorm, new Comparator<Worm>() {
            @Override
            public int compare(Worm a, Worm b) {
                return a.health - b.health;
            }
        });
        weaponChoiceAndPosition W = new weaponChoiceAndPosition();
        for(int i = 0; i < 3; i++){
            W.weapon = 0;
            W.position = enemyWorm.get(i).position;
            if(isEnemyShootable(enemyWorm.get(i))) {
                if (!isMyWormBananaBombable(enemyWorm.get(i).position) && currentWorm.health < enemyWorm.get(i).health && currentWorm.id == 2 && currentWorm.bananaBombs.count > 0) {
                    W.weapon = 2;
                    break;
                }
                else if (!isMyWormSnowballable(enemyWorm.get(i).position) && currentWorm.health < enemyWorm.get(i).health && currentWorm.id == 3 && currentWorm.snowballs.count > 0 && enemyWorm.get(i).roundsUntilUnfrozen == 0) {
                    W.weapon = 3;
                    break;
                }
                W.weapon = 1;
                break;
            }
            else if(isEnemyBananaBombable(enemyWorm.get(i)) && !isMyWormBananaBombable(enemyWorm.get(i).position) && currentWorm.id == 2 && currentWorm.bananaBombs.count > 0 && currentWorm.health < enemyWorm.get(i).health){
                W.weapon = 2;
                break;
            }
            else if(isEnemySnowballable(enemyWorm.get(i)) && !isMyWormSnowballable(enemyWorm.get(i).position) && currentWorm.id == 3 && currentWorm.snowballs.count > 0 && enemyWorm.get(i).roundsUntilUnfrozen == 0 && currentWorm.health < enemyWorm.get(i).health){
                W.weapon = 3;
                break;
//                for(int j = 0; j < 3; j++){
//                    if(isEnemyShootable(allMyWorms[j],enemyWorm.get(i))){
//                        W.weapon = 3;
//                        break;
//                    }
//                }
            }
        }
        return W;
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

    private boolean isCellInBananaBombThrowRange (Position PTarget, Position PSource) {
        boolean cellThrowable = gameState.map[PTarget.x][PTarget.y].type != CellType.DEEP_SPACE && gameState.map[PSource.x][PSource.y].type != CellType.DEEP_SPACE;
        return getEuclideanDistance(PTarget, PSource) <= 5 && cellThrowable;
    }

    private boolean isCellInSnowballBlastRange (Position PBomb, Position PTarget) {
        int distance = getEuclideanDistance(PBomb, PTarget);
        return distance <= 1;
    }

    private boolean isCellInSnowballThrowRange (Position PTarget, Position PSource) {
        int distance =getEuclideanDistance(PTarget, PSource);
        boolean cellThrowable = gameState.map[PTarget.x][PTarget.y].type != CellType.DEEP_SPACE && gameState.map[PSource.x][PSource.y].type != CellType.DEEP_SPACE;
        return (distance <= 5 && cellThrowable);
    }

    private boolean isCoordinateInBananaBombBlastRange(Position PCenter, int xTarget, int yTarget) {
        int xDifference = Math.abs(PCenter.x - xTarget);
        int yDifference = Math.abs(PCenter.y - yTarget);
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

    private boolean isCommandoAlive () {
        return (gameState.myPlayer.worms[0].health > 0);
    }
    private boolean isAgentAlive () {
        return (gameState.myPlayer.worms[1].health > 0);
    }
    private boolean isTechnologistAlive () {
        return (gameState.myPlayer.worms[2].health > 0);
    }


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
        if(getLinearDistance(P1,P2) <= maxRange && targetWorm.health > 0 && !isAnyObstacleBetween(P1,P2) && (P1.x == P2.x || P1.y == P2.y)) {
            isShootable = true;
        }
        else if(getLinearDistance(P1,P2) < maxRange && targetWorm.health > 0 && !isAnyObstacleBetween(P1,P2) ){
            isShootable = true;
        }
        return isShootable;
    }

    public boolean isEnemyShootable(Worm myWorm, Worm targetWorm) {
        boolean isShootable = false;
        int maxRange = 4;
        if(getLinearDistance(myWorm.position,targetWorm.position) <= maxRange && targetWorm.health > 0 && !isAnyObstacleBetween(myWorm.position,targetWorm.position) && (myWorm.position.x == targetWorm.position.x || myWorm.position.y == targetWorm.position.y)) {
            isShootable = true;
        }
        else if(getLinearDistance(myWorm.position,targetWorm.position) < maxRange && targetWorm.health > 0 && !isAnyObstacleBetween(myWorm.position,targetWorm.position)){
            isShootable = true;
        }
        return isShootable;
    }

    private boolean isAnyMyWormInPosition(int x, int y){
        for(int i = 0; i < 3; i++){
            if((allMyWorms[i].position.x == x) && (allMyWorms[i].position.y == y) && (allMyWorms[i].health > 0)){
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
                if(gameState.map[P1.y][P1.x + a].type == CellType.DIRT || isAnyMyWormInPosition(P1.x + a,P1.y)){
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
                if(gameState.map[P1.y + b][P1.x].type == CellType.DIRT || isAnyMyWormInPosition(P1.x,P1.y + b)){
                    obstacle = true;
                    break;
                }
            }
        }
        else if(Math.abs(P1.x - P2.x) == Math.abs(P1.y - P2.y)){
            int val = Math.abs(P1.y - P2.y);
            for(int i = 1; i < val; i++){
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
                if(gameState.map[P1.y + b][P1.x + a].type == CellType.DIRT || isAnyMyWormInPosition(P1.x + a,P1.y + b)){
                    obstacle = true;
                    break;
                }
            }
        }
        return obstacle;
    }

    public int getDamageIfEnemyShootable(Worm targetWorm) {
        if(isEnemyShootable(targetWorm)) {
            return 8;
        } else return 0;
    }

    public Position getClosestEnemy() {
        int closestDistance = 9999;
        Position P3 = new Position();
        for(int i=0;i<allMyWorms.length;i++) {
            for(int j=0;j<allOpponentWorms.length;j++) {
                if(getShortestDistance(getVertexForDijkstra(allMyWorms[i].position, allOpponentWorms[j].position)) < closestDistance && allOpponentWorms[j].health > 0) {
                    closestDistance = getLinearDistance(allMyWorms[i].position, allOpponentWorms[j].position);
                    P3.x = allOpponentWorms[j].position.x;
                    P3.y = allOpponentWorms[j].position.y;
                }
            }
        }
        return P3;
    }

//    private List<Cell> getAllCellsWithPowerup(){
//        ArrayList<Cell> powerup = new ArrayList<Cell>();
//        for(int i = 0; i < gameState.map.length; i++){
//            for(int j = 0; j < gameState.map.length; j++){
//                if(gameState.map[i][j].powerUp != null){
//                    powerup.add(gameState.map[i][j]);
//                }
//            }
//        }
//        return powerup;
//    }
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

    public Position createPosition (int xCoordinate, int yCoordinate) {
        Position P = new Position();
        P.x = xCoordinate;
        P.y = yCoordinate;
        return P;
    }

    public int getCellDamageFromBananaBomb(Position PCenter, Position PTarget) {
        int bananaBombMaxDamage = 20;
        int bananaBombMaxRange = 2;
        // menghitung damage yang dapat diterima worms bergantung pada jaraknya dengan posisi pelemparan Banana Bomb
        int[] damageTier = new int[4];
        for (int distance = 0; distance < 4; distance++) {
            if (distance > 2) {
                damageTier[distance] = 0;
            }
            else {
                damageTier[distance] = (int) bananaBombMaxDamage * ((bananaBombMaxRange+1 - distance)/(bananaBombMaxDamage+1));
            }
        }
        int distanceFromPointZero = getEuclideanDistance(PCenter, PTarget);

        return damageTier[distanceFromPointZero % 4];
    }

    public int getMaxBananaBombDamageByPlayer () {
        // mengembalikan damage maksimum yang dapat di-inflict ke musuh
        // dengan melakukan pelemparan Banana Bomb pada suatu round dengan asumsi womrs musuh tidak bergerak
        // Prekondisi: Agent dapat menerima command, masih ada Banana Bomb

        // mendapatkan posisi Agent dan worms
        Position PAgent = gameState.myPlayer.worms[1].position;

        Position[] enemyPosition = getEnemyPosition();
        Position[] myWormsPosition = getMyWormsPosition();

        // mendapatkan jarak Agent ke masing-masing musuh
        int[] enemyDistances = new int[3];
        for (int k = 0; k < 3; k++) {
            if (enemyPosition[k].x >= 0){
                enemyDistances[k] = getEuclideanDistance(PAgent, enemyPosition[k]);
            }
            else {
                enemyDistances[k] = 999;
            }
        }

        //initialize damageMap
        int[][] damageMap = new int[33][33];    // mencatat pemetaan damage yand ditimbulkan ke worms musuh
        for (int i = 0; i < 33; i++) {
            for (int j = 0; j < 33; j++) {
                damageMap[i][j] = 0;
            }
        }

        for (int worm = 0; worm < 3; worm++) {
            if (enemyDistances[worm] > 7 || gameState.opponents[0].worms[worm].health <= 0) {
                // tidak perlu mengevaluasi damage terhadap worm musuh ini
            }
            else {
                // mengevaluasi damage terhadap worm musuh; mencatat damage pada damageMap
                Position pCurrentEnemyPosition = enemyPosition[worm];
                int xCenter = pCurrentEnemyPosition.x; int yCenter = pCurrentEnemyPosition.y;

                for (int xRelative = -2; xRelative <= 2; xRelative++) {
                    for (int yRelative = -2; yRelative <= 2; yRelative++) {
                        // cell tempat bomb akan dilempar yang akan dievaluasi damage yang dapat diberikan
                        Position PEval = createPosition(xCenter+xRelative, yCenter+yRelative);
                        if (isCellInBananaBombBlastRange(PEval, pCurrentEnemyPosition)) {
                            if (isCellInBananaBombThrowRange(PEval, PAgent)) {
                                int currentEnemyInflictedDamage = getCellDamageFromBananaBomb(PEval, pCurrentEnemyPosition);
                                damageMap[xCenter+xRelative][yCenter+yRelative] += currentEnemyInflictedDamage;
                            }
                        }
                    }
                }
                // selesai mengevaluasi cell di sekitar worm yang dapat memberikan damage
            }
        }

        List<Position> candidatePosition = new ArrayList<Position>();
        int maxDamageInflicted = 0;
        // mengevaluasi colateral damage pada worms sendiri
        for (int i = 0; i < 33; i++) {
            for (int j = 0; j < 33; j++) {
                if (damageMap[i][j] != 0) {
                    // cell telah dievaluasi sebelumnya (considerable)
                    Position PEval = createPosition(i, j);
                    int alliesInflictedDamage = 0;
                    for (int k = 0; k < 3; k++) {
                        alliesInflictedDamage += getCellDamageFromBananaBomb(PEval, myWormsPosition[k]);
                    }
                    damageMap[i][j] -= alliesInflictedDamage;

                    if (damageMap[i][j] > maxDamageInflicted) {
                        // ada cell baru yang memberi damage terbesar sejauh ini
                        maxDamageInflicted = damageMap[i][j];
                        candidatePosition.clear();
                        candidatePosition.add(createPosition(i, j));
                    }
                    else if (damageMap[i][j] == maxDamageInflicted) {
                        // ada cell lain yang sama memberikan damage sama baiknya
                        candidatePosition.add(createPosition(i, j));
                    }
                }
            }
        }
        // update atribut di class
        candidateBananaBombPositionByPlayer = candidatePosition;
        return maxDamageInflicted;
    }

    public int getMaxBananaBombDamageByEnemy () {
        // mengembalikan damage maksimum yang dapat di-inflict ke Player
        // dengan melakukan pelemparan Banana Bomb pada suatu round dengan asumsi womrs musuh tidak bergerak
        // Prekondisi: Agent musuh dapat menerima command, masih ada Banana Bomb

        // mendapatkan posisi Agent musuh dan worms
        Position PAgent = gameState.opponents[0].worms[1].position;

        Position[] myWormsPosition = getMyWormsPosition();
        Position[] enemyPosition = getEnemyPosition();

        // mendapatkan jarak Agent musuh ke masing-masing worm kita
        int[] myDistances = new int[3];
        for (int k = 0; k < 3; k++) {
            if (myWormsPosition[k].x >= 0){
                myDistances[k] = getEuclideanDistance(PAgent, myWormsPosition[k]);
            }
            else {
                myDistances[k] = 999;
            }
        }

        //initialize damageMap
        int[][] damageMap = new int[33][33];    // mencatat pemetaan damage yand ditimbulkan ke worms musuh
        for (int i = 0; i < 33; i++) {
            for (int j = 0; j < 33; j++) {
                damageMap[i][j] = 0;
            }
        }

        for (int worm = 0; worm < 3; worm++) {
            if (myDistances[worm] > 7 || gameState.myPlayer.worms[worm].health <= 0) {
                // tidak perlu mengevaluasi damage terhadap worm Player yang satu ini
                continue;
            }
            else {
                // mengevaluasi damage terhadap worm Player; mencatat damage pada damageMap
                Position pCurrentWormPosition = myWormsPosition[worm];
                int xCenter = pCurrentWormPosition.x; int yCenter = pCurrentWormPosition.y;

                for (int xRelative = -2; xRelative <= 2; xRelative++) {
                    for (int yRelative = -2; yRelative <= 2; yRelative++) {
                        // cell tempat bomb akan dilempar yang akan dievaluasi damage yang dapat diterima
                        Position PEval = createPosition(xCenter+xRelative, yCenter+yRelative);
                        if (isCellInBananaBombBlastRange(PEval, pCurrentWormPosition)) {
                            if (isCellInBananaBombThrowRange(PEval, PAgent)) {
                                int currentWormInflictedDamage = getCellDamageFromBananaBomb(PEval, pCurrentWormPosition);
                                damageMap[xCenter+xRelative][yCenter+yRelative] += currentWormInflictedDamage;
                            }
                        }
                    }
                }
                // selesai mengevaluasi cell di sekitar worm Player yang dapat memberikan damage kepada Player
            }
        }

        List<Position> candidatePosition = new ArrayList<Position>();
        int maxDamageInflicted = 0;
        // mengevaluasi colateral damage pada worms musuh
        for (int i = 0; i < 33; i++) {
            for (int j = 0; j < 33; j++) {
                if (damageMap[i][j] != 0) {
                    // cell telah dievaluasi sebelumnya (considerable)
                    Position PEval = createPosition(i, j);
                    int opponentAlliesInflictedDamage = 0;
                    for (int k = 0; k < 3; k++) {
                        opponentAlliesInflictedDamage += getCellDamageFromBananaBomb(PEval, enemyPosition[k]);
                    }
                    damageMap[i][j] -= opponentAlliesInflictedDamage;

                    if (damageMap[i][j] > maxDamageInflicted) {
                        // ada cell baru yang memberi damage terbesar sejauh ini
                        maxDamageInflicted = damageMap[i][j];
                        candidatePosition.clear();
                        candidatePosition.add(createPosition(i, j));
                    }
                    else if (damageMap[i][j] == maxDamageInflicted) {
                        // ada cell lain yang sama memberikan damage sama baiknya
                        candidatePosition.add(createPosition(i, j));
                    }
                }
            }
        }
        // update atribut di class
        candidateBananaBombPositionByEnemy = candidatePosition;
        return maxDamageInflicted;
    }

    public void calculateImpactOfBeingFrozen () {
        // Meng-update nilai impactOfBeingFrozen
        // menghitung Impact yang diterima oleh setiap worm jika ter-freeze pada round tertentu
        // nilai impact dilihat dari damage yang dapat diberikan worm lawan yang berada di radius 6

        //distanceOneOneOne[i][j] adalah jarak worm Player i ke worm opponent j
        int[][] distanceOneOnOne = new int[3][3];
        int[] impactOnWorm = new int[6];

        // Posisi setiap worm
        Position[] allyPosition = getMyWormsPosition();
        Position[] enemyPosition = getEnemyPosition();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 6; j++) {
                distanceOneOnOne[i][j] = getEuclideanDistance(allyPosition[i], enemyPosition[j]);
            }
        }

        // besar konstanta impact yang dapat diberikan sebuah worm
        int impactConstant = 6;

        for (int k = 0; k < 6; k++) {
            impactOnWorm[k] = 0;
            if (k < 3) {
                // evaluasi impact terhadap worm Player
                for (int t = 0; t < 3; t++) {
                    // jumlah impact oleh ketiga musuh
                    if (distanceOneOnOne[k][t] <= 6) {
                        impactOnWorm[k] += impactConstant;
                    }
                }
            }
            else {
                // evaluasi impact terhadap worm musuh
                for (int t = 0; t < 3; t++) {
                    if (distanceOneOnOne[t][k%3] <= 6) {
                        impactOnWorm[k] += impactConstant;
                    }
                }
            }
        }

        impactOfBeingFrozen = impactOnWorm;
    }

    public int[] getSnowballImpact () {
        // Mengembalikan "impact" terbesar yang dapat di-inflict kepada musuh dengan pelemparan Snowball
        // dengan asumsi worms musuh tidak bergerak
        // "Impact" dihitung dengan mengevaluasi gambaran kasar kemungkinan serangan dalam 5 round ke depan
        // Prekondisi:
        // Technologist dapat menerima command dan masih ada Snowball

        // Preparation
        calculateImpactOfBeingFrozen();

        // mendapatkan posisi Agent dan worms
        Position PTechnologist = gameState.myPlayer.worms[2].position;

        Position[] enemyPosition = getEnemyPosition();
        Position[] myWormsPosition = getMyWormsPosition();

        // mendapatkan jarak Technologist ke masing-masing musuh
        int[] enemyDistances = new int[3];
        for (int k = 0; k < 3; k++) {
            if (enemyPosition[k].x >= 0){
                enemyDistances[k] = getEuclideanDistance(PTechnologist, enemyPosition[k]);
            }
            else {
                enemyDistances[k] = 999;
            }
        }

        // initialize snowballableMap
        // mencatat apakah pelemparan snowball pada Cell [i][j] dapat membekukan worm k
        // 0-2: player worms, 3-5: enemy worms
        boolean[][][] snowballableMap = new boolean[33][33][6];
        for (int i = 0; i < 33; i++) {
            for (int j = 0; j < 33; j++) {
                for (int k = 0; k < 6; k++) {
                    snowballableMap[i][j][k] = FALSE;
                }
            }
        }

        // mencari efek pelemparan snowball terhadap semua Player
        for (int k = 0; k < 6; k++) {
            Position PCurrentWorm;
            if (k < 3) { // worms Player
                PCurrentWorm = myWormsPosition[k];
            }
            else { // worms musuh
                PCurrentWorm = enemyPosition[k%3];
            }
            int xCurrrent = PCurrentWorm.x; int yCurrent = PCurrentWorm.y;
            if (xCurrrent == -999) { // worm sudah mati
                continue;
            }

            // tinjau Cells di sekitar worm
            for (int xRelative = -1; xRelative <= 1; xRelative++) {
                for (int yRelative = -1; yRelative <= 1; yRelative++) {
                    Position PEval = createPosition(xCurrrent+xRelative, yCurrent+yRelative);
                    if (isCellInSnowballThrowRange(PEval, PTechnologist)) {
                        snowballableMap[xCurrrent][yCurrent][k] = TRUE;
                        break;
                    }
                }
            }
        }

        List<Position> snowballCandidatePosition = new ArrayList<>();
        List<Position> snowballCandidatePositionEnemy = new ArrayList<>();
        int maxImpactInflicted = 0; // semakin besar semakin baik untuk Player
        int minImpactInflicted = 0; // semakin kecil semakin baik untuk Enemy
        int[] impactPerWorm = impactOfBeingFrozen;
        int[][] impactMap = new int[33][33];

        for (int i = 0; i < 33; i++){
            for (int j = 0; j < 33; j++) {
                impactMap[i][j] = 0;
                // evaluasi Impact yang disebabkan pelemparan snowball pada setiap Cell
                for (int k = 0; k < 6; k++) {
                    if (snowballableMap[i][j][k]) {
                        if (k < 3) {
                            // impact terhadap worm sendiri (bad)
                            impactMap[i][j] -= impactPerWorm[k];
                        }
                        else {
                            // impact terhadap worm opponen (good)
                            impactMap[i][j] += impactPerWorm[k];
                        }
                    }
                }
                if (impactMap[i][j] > 0) {
                    // Cell ini considerable untuk dilakukan pelemparan snowball
                    if (impactMap[i][j] > maxImpactInflicted) {
                        // mencatat Cell terbaik yang baru
                        maxImpactInflicted = impactMap[i][j];
                        snowballCandidatePosition.clear();
                        snowballCandidatePosition.add(createPosition(i,j));
                    }
                    else if (impactMap[i][j] == maxImpactInflicted) {
                        // menambahkan Cell yang sama baiknya ke ArrayList
                        snowballCandidatePosition.add(createPosition(i,j));
                    }
                }
                else if (impactMap[i][j] < 0) {
                    // Cell ini considerable untuk dilakukan pelemparan snowball
                    if (impactMap[i][j] < minImpactInflicted) {
                        // mencatat Cell terbaik yang baru
                        minImpactInflicted = impactMap[i][j];
                        snowballCandidatePositionEnemy.clear();
                        snowballCandidatePositionEnemy.add(createPosition(i,j));
                    }
                    else if (impactMap[i][j] == minImpactInflicted) {
                        // menambahkan Cell yang sama baiknya ke ArrayList
                        snowballCandidatePositionEnemy.add(createPosition(i,j));
                    }
                }
            }
        }

        candidateSnowballPositionByPlayer = snowballCandidatePosition;
        candidateSnowballPositionByEnemy = snowballCandidatePositionEnemy;

        int[] maxMinImpact = {maxImpactInflicted, -1*minImpactInflicted};
        return maxMinImpact;
    }

    public Position getClosestPowerup() {
        Position P3 = new Position();
        P3.x = 16;
        P3.y = 16;
        Position tempPowerupPosition = new Position();
        int minDistance = 9999;
        for(int i=0;i<allMyWorms.length;i++) {
            for(int j=0;j<presummedPowerupCells.length;j++) {
                if(presummedPowerupCells[j] == null) {
                    break;
                } else {
                    tempPowerupPosition.x = presummedPowerupCells[j].x;
                    tempPowerupPosition.y = presummedPowerupCells[j].y;
                    if (presummedPowerupCells[j].powerUp != null && getShortestDistance(getVertexForDijkstra(allMyWorms[i].position, tempPowerupPosition)) < minDistance) {
                        minDistance = getShortestDistance(getVertexForDijkstra(allMyWorms[i].position, tempPowerupPosition));
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
        Position P = getClosestEnemy();
        int health = 0;
        for(int i = 0; i < 3; i++){
            if(allOpponentWorms[i].position == P){
                health = allMyWorms[i].health;
                break;
            }
        }
        return health;
    }

    public int getMaxDamagePossibleGiven() {
        int maxDamage = 0;
        Worm[] recievers = allOpponentWorms;
        if(currentWorm.id == 2 && currentWorm.bananaBombs.count > 0) {
            if(maxDamage < getMaxBananaBombDamageByPlayer()) {
                maxDamage = getMaxBananaBombDamageByPlayer();
            }
        } else if(currentWorm.id == 3 && currentWorm.snowballs.count > 0) {
            if(maxDamage < getSnowballImpact()[0]) {
                maxDamage = getSnowballImpact()[0];
            }
        } else {
            for (int i = 0; i < recievers.length; i++) {
                if (maxDamage < getDamageIfEnemyShootable(recievers[i])) {
                    maxDamage = 8;
                    break;
                }
            }
        }
        return maxDamage;
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