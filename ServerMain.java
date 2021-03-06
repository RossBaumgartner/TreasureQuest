import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.io.*;

public class ServerMain implements ActionListener, Runnable {
  //properties
  SuperSocketMaster ssm;
  String strMsg;
  String strCurrentMap;
  int mapNumber;
  int PlayersJoined = 0;
  int PlayerStats [][] = new int [4][3];
  int PlayserHealth[] = new int [4];
  String strPlayerAttackDamage[][];
  boolean playerPresent [] = {false, false, false, false};
  boolean playerActivator [] = {false, false, false, false};
  boolean playersAlive[] = {true, false, false, false};
  int intActivatorPlayers = 0;
  int intNewMapPlayers = 0;
  Timer timer;
  
  //extras
  JPanel panel;
  JFrame frame;
  JButton btnSend;
  JTextField txtInput;
  
  FileReader enemyDataFile;
  BufferedReader enemyDataReader;
  static Map currentMap;
  enemy Enemies [];
  int activateTiles [][];
  int intEnemyCount = 0;
  int intEnemyAlive = -10;
  
  
  public void run(){
    //initialize frame+panel+button+textfield
    /*
    panel = new JPanel();
    panel.setLayout(null);
    frame = new JFrame("Server");
    frame.setDefaultCloseOperation(3);
    panel.setPreferredSize(new Dimension(400, 300));
    
    txtInput = new JTextField();
    txtInput.setSize(300, 50);
    txtInput.setLocation(20, 20);
    
    btnSend = new JButton("Send msg");
    btnSend.setLocation(20, 70);
    btnSend.setSize(80, 30);
    btnSend.addActionListener(this);
    
    panel.add(btnSend);
    panel.add(txtInput);
    
    frame.setContentPane(panel);
    frame.pack();
    frame.setVisible(true);
    */
    
    try {
      enemyDataFile = new FileReader(strCurrentMap+"enemies.txt");
      enemyDataReader = new BufferedReader(enemyDataFile);
      currentMap = new Map (strCurrentMap + ".csv");
      currentMap.renderBoundMap();
    }catch (IOException e) {
      System.out.println ("exception: " +e);
    }
    
    ssm = new SuperSocketMaster (6112, this);
    ssm.connect();
    
    try {
      String Tiles [] = enemyDataReader.readLine().split(", ");
      activateTiles = new int [Tiles.length][2];
      for (int Count = 0; Count < Tiles.length; Count ++) {
        String strStorage [] = Tiles [Count].split("_");
        activateTiles [Count][0] = Integer.parseInt(strStorage [0]);
        activateTiles [Count][1] = Integer.parseInt(strStorage [1]);
        //System.out.println (activateTiles [Count][0] + "," + activateTiles [Count][1]);
        
      }
    }catch (IOException e) {
    }
    
    // get player attack damage
    try{
      FileReader dataFile = new FileReader("data_attackdamage.txt");
      BufferedReader readData = new BufferedReader(dataFile);
      strPlayerAttackDamage = new String[Integer.parseInt(readData.readLine())][2];
      for(int line=0;line<strPlayerAttackDamage.length; line++){
        String linedata[] = readData.readLine().split(";");
        strPlayerAttackDamage[line][0] = linedata[0];
        strPlayerAttackDamage[line][1] = linedata[1];
        //System.out.println(linedata[0] + ":" + linedata[1]);
      }
    }catch(Exception e){
      System.out.println(e);
    }
    
    
    
    timer = new Timer (100/6, this);
    timer.start();
  }
  
  //methods
  public void actionPerformed (ActionEvent evt) throws NullPointerException {

    if (evt.getSource () == ssm) {
      strMsg = ssm.readText();
      String strData [] = strMsg.split (",");
      int intPlayer;
      
      if(strData [0].equals ("joined")) {
        ssm.sendText ("server,enemyCount," + intEnemyCount);
        ssm.sendText ("server,assign," + PlayersJoined);
        playerPresent [PlayersJoined] = true;
        for (int Count = 0; Count < activateTiles.length; Count ++) {
          ssm.sendText ("server,updateTiles," + activateTiles [Count][0] + "," + activateTiles [Count][1] + "," + 1);
        }
        PlayersJoined ++;
        
      }else if (strData [2].equals ("stats")) {
        intPlayer = Integer.parseInt (strData [1]);
        PlayerStats [intPlayer][0] = Integer.parseInt(strData [3]); //x
        PlayerStats [intPlayer][1] = Integer.parseInt(strData [4]); //y
        PlayerStats [intPlayer][2] = Integer.parseInt(strData [5]); //angle
      }else if (strData [2].equals ("kill")){
        try {
          Enemies [Integer.parseInt(strData [3])].kill(); 
        }catch (ArrayIndexOutOfBoundsException e) {
        }
      }else if (strData [2].equals ("activate")) {
        playerActivator [Integer.parseInt(strData [1])] = true;
        if (strData [3].equals("enemies")) {
          intActivatorPlayers ++;
        }else if (strData [3].equals("nextmap")) {
          intNewMapPlayers ++;
        }
        ssm.sendText ("server,acknowledgeactivate," + strData [1]);
      }else if (strData [2].equals ("deactivate")) {
        playerActivator [Integer.parseInt(strData [1])] = false;
        if (strData [3].equals("enemies")) {
          intActivatorPlayers --;
        }else if (strData [3].equals("nextmap")) {
          intNewMapPlayers --;
        }
        
        ssm.sendText ("server,acknowledgedeactivate," + strData [1]);
      }else if(strData[2].equals("sendatk")){//server,sendattack,p#,atkname
        //player sent an attack request
        String attack = strData[3];
        //System.out.println("SERVER// REQUEST: " + strMsg);
        //System.out.println(attack);
        intPlayer = Integer.parseInt (strData [1]);
        
        int attackRange = 200; //enter values for how far each attack hits in pixels
        int attackAngle = 60;
        
        //check if enemy is hit
        for(int enemy=0; enemy<intEnemyCount; enemy++){
          if(Enemies[enemy].dblDistance[intPlayer] < attackRange){
            //this enemy is within range
            //get angle of enemy relative to player player
            double p_enemy_angle = (Math.toDegrees (Enemies [enemy].findAngle(PlayerStats[intPlayer][0], PlayerStats[intPlayer][1])) + 180) % 360;
            int angleLeft = PlayerStats[intPlayer][2]-(attackAngle/2);
            int angleRight = PlayerStats[intPlayer][2]+(attackAngle/2);
            boolean blnInRange = false;
            
            if (angleRight > 360) {
              if (p_enemy_angle >= angleLeft && p_enemy_angle <= 360) {
                blnInRange = true;
              }else if (p_enemy_angle >= 0 && p_enemy_angle <= angleRight - 360) {
                blnInRange = true;
              }
            }else if (angleLeft < 0) {
              if (p_enemy_angle >= 0 && p_enemy_angle <= angleRight) {
                blnInRange = true;
              }else if (p_enemy_angle >= angleLeft + 360 && p_enemy_angle <= 360) {
                blnInRange = true;
              }
            }else {
              if (p_enemy_angle >= angleLeft && p_enemy_angle <= angleRight) {
                blnInRange = true;
              }
            }
            
            double time_since_last_hit = (System.nanoTime()-Enemies[enemy].timeSinceAttacked)/1e6; //get the time since enemy was last damaged (in MS)
            if(blnInRange && Enemies [enemy].alive == true){
              if(time_since_last_hit > 300){ //allow enemy to recover
                
                int intDamage = 30;
                //find the attack that was sent
                //set intDamage to the damage found in the attack data file.
                for(int count=0;count<strPlayerAttackDamage.length; count++){
                  if(attack.equals(strPlayerAttackDamage[count][0])){
                    intDamage = Integer.parseInt(strPlayerAttackDamage[count][1]);
                    //System.out.println("Attack '" + strPlayerAttackDamage[count][0] + "' " + intDamage + " damage");
                  }
                }
                
                Enemies[enemy].intHealth -= intDamage;
                //System.out.println("Damaged " + enemy);
                Enemies[enemy].timeSinceAttacked = System.nanoTime();
                ssm.sendText("server,enemyDamaged," + enemy); //
                if(Enemies [enemy].intHealth <= 0){
                  Enemies [enemy].kill();
                  //System.out.println ("Enemy " + enemy + " killed " + Math.round(Enemies [enemy].dblDistance [intPlayer]));
                  //delay 2 seconds then enemy disappear
                }
              }
            }
            
          }
        }
      }else if(strData[2].equals("iamdeadlol")){
        playersAlive[Integer.parseInt(strData[1])] = false;
        //stop enemy from targeting
        for(int enemy=0;enemy<Enemies.length;enemy++){
          if(Enemies[enemy].Target == Integer.parseInt(strData[1])){
            //player is a target
            Enemies[enemy].Target = 10;
            Enemies[enemy].targetting = false;
            PlayerStats[Integer.parseInt(strData[1])][0] = 10000;
            PlayerStats[Integer.parseInt(strData[1])][1] = 10000;
          }
        }
      }else if(strData[2].equals("iamaliveayyy")){
        playersAlive[Integer.parseInt(strData[1])] = true;
      }
      
    }
    if (evt.getSource () == timer) {
      //reset map if no players are alive
      /*
      int alive=0;
      for(int p=0;p<4;p++){
        if(playersAlive[p]){
          alive++;
          //System.out.println(p + " is alive");
        }
      }
      if(alive==0){
        ssm.sendText("server,changeMap,map" + mapNumber + ",600,600");
      }*/
      
      //ememy
      if (intEnemyAlive > 0) {
        for (int Count = 0; Count < intEnemyCount; Count ++) {
          if (Enemies [Count].alive == true) {
            
            for (int Count1 = 0; Count1 < 4; Count1 ++) {
              if (playerPresent [Count1] == true) {
                Enemies [Count].findRange (PlayerStats [Count1][0], PlayerStats [Count1][1], Count1);
              }
            }
            if (Enemies [Count].targetting == false) {
              Enemies [Count].attemptActivate();
            }else {
              //stop enemy from moving if enemy is attacked
              double time_since_last_hit = (System.nanoTime()-Enemies[Count].timeSinceAttacked)/1e6;
              if(time_since_last_hit > 300){ //stop player from moving for 0.6 second. 
                Enemies [Count].move (PlayerStats [Enemies [Count].Target][0], PlayerStats [Enemies [Count].Target][1]);
                if (Enemies [Count].attacking == true) {
                  Enemies [Count].attacking = false;
                  ssm.sendText ("server,enemyAttack," + Count + "," + Enemies[Count].Target + "," + Enemies[Count].intAttack);
                  //System.out.println ("server,enemyAttack," + Count + "," + Enemies[Count].Target + "," + Enemies[Count].intAttack);
                }
              }
            }
            ssm.sendText ("server,enemy," + Count + "," + Enemies [Count].intX + "," + Enemies [Count].intY + "," + (int) (Math.toDegrees(Enemies [Count].dblAngle)) + "," + Enemies[Count].strType);
            
          }else if (Enemies [Count].justKilled == true) {
            Enemies [Count].justKilled = false;
            Enemies [Count].alive = false;
            ssm.sendText("server,enemy," + Count + ",0,0,0,none");
            intEnemyAlive  --;
            System.out.println (intEnemyAlive);
          }
        }
      }else if (intEnemyAlive == 0) {
        try {
          resetSpawning();
        }catch (IOException e) {}
        intEnemyAlive = -10;
      }
      
      //activate
      if (intActivatorPlayers == PlayersJoined && intEnemyAlive == -10 && PlayersJoined != 0) {
        for (int Count = 0; Count < activateTiles.length; Count ++) {
          ssm.sendText ("server,updateTiles," + activateTiles [Count][0] + "," + activateTiles [Count][1] + "," + 0);
        }
        
        try {
          generateEnemies();
        }catch (IOException e) {}
        
      }else if (intNewMapPlayers == PlayersJoined && PlayersJoined != 0) {
        ssm.sendText ("server,changeMap,map" + (mapNumber + 1) + ",600,600");
        try {
          enemyDataReader.close();
          enemyDataFile.close();
        }catch (IOException e) {}
      }
    }
  }
  
  //enemies
  public void generateEnemies () throws IOException {
    intEnemyCount = Integer.parseInt(enemyDataReader.readLine());
    intEnemyAlive = intEnemyCount;
    String enemyData [] = new String [5];
    Enemies = new enemy [intEnemyCount];
    
    for (int Count = 0; Count < intEnemyCount; Count ++) {
      enemyData = (enemyDataReader.readLine()).split(", ");
      Enemies [Count] = new enemy (Integer.parseInt(enemyData [0]), Integer.parseInt(enemyData [1]), enemyData [2], Integer.parseInt(enemyData [3]), Integer.parseInt(enemyData [4]));
    }
    
    String Tiles [] = enemyDataReader.readLine().split(", ");
    for (int Count = 0; Count < Tiles.length; Count ++) {
      String strStorage [] = Tiles [Count].split("_");
      ssm.sendText ("server,updateTiles," + strStorage [0] + "," + strStorage [1] + "," + 0);

      //send enemy type
      ssm.sendText("server,enemyType," + Enemies[Count].strType);
    }
    
    ssm.sendText ("server,enemyCount," + intEnemyCount);
  }
  
  public void resetSpawning () throws IOException {
    String Tiles [] = enemyDataReader.readLine().split(", ");
    
    for (int Count = 0; Count < Tiles.length; Count ++) {
      String strStorage [] = Tiles [Count].split("_");
      ssm.sendText ("server,updateTiles," + strStorage [0] + "," + strStorage [1] + "," + 1);
    }
    
    Tiles = enemyDataReader.readLine().split(", ");
    if (!Tiles [0].equals ("1000_1000")) {
      activateTiles = new int [Tiles.length][2];
      for (int Count = 0; Count < Tiles.length; Count ++) {
        String strStorage [] = Tiles [Count].split("_");
        activateTiles [Count][0] = Integer.parseInt(strStorage [0]);
        activateTiles [Count][1] = Integer.parseInt(strStorage [1]);
        
        ssm.sendText ("server,updateTiles," + strStorage [0] + "," + strStorage [1] + "," + 1);
      }
    }else {
      enemyDataReader.close();
      enemyDataFile.close();
    }
  }
  
  //constructor
  public ServerMain (String currentMapFile) {
    //MOVED TO RUN() METHOD
    //SINCE THIS IS A THREAD
    this.strCurrentMap = currentMapFile;
    mapNumber = Integer.parseInt(currentMapFile.substring(3,4));
  }
}