package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.util.*;

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

    private Position getPos(int x, int y){
        Position P = new Position();
        P.x = x;
        P.y = y;
        return P;
    }

    public Command run(){
        Position P;
        if(isPowerUpAvailable()){
            P = getClosestPowerup();
        }
        else{
            P = getClosestEnemy();
        }
        if(allOpponentWorms[2].health > 0){
            if(currentWorm.id == 1 || currentWorm.id == 2) {
                P = allOpponentWorms[2].position;
            }
            else{
                P = allOpponentWorms[1].position;
            }
        }
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

    private boolean isPowerUpAvailable(){
        for(int i = 0; i < presummedPowerupCells.length; i++){
            if(presummedPowerupCells[i] != null){
                return true;
            }
        }
        return false;
    }

    private boolean isEnemyBananaBombable(Worm targetWorm){
        if(targetWorm.health > 0) {
            int distance = getEuclideanDistance(currentWorm.position, targetWorm.position);
            return distance == 5;
        }
        return false;
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
        if(targetWorm.health > 0) {
            int distance = getEuclideanDistance(currentWorm.position, targetWorm.position);
            return distance == 5;
        }
        return false;
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
                if (!isMyWormBananaBombable(enemyWorm.get(i).position) && currentWorm.id == 2 && currentWorm.bananaBombs.count > 0) {
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
            else if(isEnemyBananaBombable(enemyWorm.get(i)) && !isMyWormBananaBombable(enemyWorm.get(i).position) && currentWorm.id == 2 && currentWorm.bananaBombs.count > 0){
                W.weapon = 2;
                break;
            }
            else if(isEnemySnowballable(enemyWorm.get(i)) && !isMyWormSnowballable(enemyWorm.get(i).position) && currentWorm.id == 3 && currentWorm.snowballs.count > 0 && enemyWorm.get(i).roundsUntilUnfrozen == 0){
                W.weapon = 3;
                break;
            }
        }
        return W;
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
            if(!isSejajarSbX(Psrc, Pdest) && !isSejajarSbY(Psrc, Pdest)) {
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
            if(allOpponentWorms[i].health < minHealth && allOpponentWorms[i].health > 0){
                minHealth = allOpponentWorms[i].health;
                enemy = allOpponentWorms[i];
            }
        }
        return enemy;
    }

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