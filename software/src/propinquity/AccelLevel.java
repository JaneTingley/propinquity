package propinquity;

import processing.core.*;
import processing.xml.*;
import propinquity.hardware.*;
import ddf.minim.*;
import java.util.*;
import java.lang.Math;
/**
 * The ProxLevel is the "original" game mechanic for Propinquity, players score by being in proximity to the opponent's patches. The opponent's patches may not be on at all times. There are also cooperative and versus rounds and there are pauses between rounds. It supports loading a level from an XML file and can have multiple songs per level.
 *
 * It is currently not in use, but support two scoring zones.
 *
 */
public class AccelLevel extends Level {

    static int ORB_WARNING = 400;
    static int ORB_THRESHOLD = 600;
  
    static int TOTAL_LEN = 180000; //2min;

    Color orbcolor;
    int orblives;
    
    long[] lastScoreTime;
    long[] lastScoreTimePauseDiff;

    AudioPlayer song;
    AudioSample gong;
    AudioSample dingding;

    int songTransitionCount;
    boolean newSongFlag;
    Vector<AudioPlayer> songs;

    String songFile;
    int songBPM;

    VolumeFader fader;

    String name;

    boolean coop, lastCoop;
    boolean running;
    
    int coopScore;

    long startTime, startTimeDiff;
    long protectedUntil;

    boolean useBackgroundColor;
    private Patch orb;
	private boolean timeout;

    public AccelLevel(Propinquity parent, Hud hud, Sounds sounds, String levelfile, Player[] players) {
        super(parent, hud, sounds, players);

        orbcolor = Color.green();
        name = levelfile;
        
        gong = sounds.getGong();
        dingding = sounds.getDingDing();
        song = sounds.loadSong("11 Besouro.mp3");
        
        lastScoreTime = new long[players.length];
        lastScoreTimePauseDiff = new long[players.length];
        coopScore = 0;

        startTime = -1;

        useBackgroundColor = true;

        fader = new VolumeFader();

        reset();
}

    public void useBackgroundColor(boolean useBackgroundColor) {
        this.useBackgroundColor = useBackgroundColor;
    }

    public void pause() {
	song.pause();
        running = false;
        for(int i = 0;i < players.length;i++) {
            players[i].pause();
            lastScoreTimePauseDiff[i] = parent.millis()-lastScoreTime[i];
        }
        startTimeDiff = parent.millis()-startTime;
    }

    public void start() {

            
      for(int i = 0;i < players.length;i++) {
            players[i].start();
            lastScoreTime[i] = parent.millis()-lastScoreTimePauseDiff[i];
        }
        running = true;
        if(startTime == -1) startTime = parent.millis();
        else startTime = parent.millis()-startTimeDiff;
        song.play();
        song.setGain(-50);
        

        // New stuff here
        // Orb defender
        this.orb = players[0].patches[0];
        this.orb.setActivationMode(Mode.PROX | Mode.ACCEL_INT0 | Mode.ACCEL_INT1);
        this.orb.setColor(this.orbcolor);
        this.orb.setActive(true);
        this.orb.setAccelConfig(0);
        timeout = false;

        // Attackers (treat as one single player with multiple patches)
        players[1].configurePatches(Mode.OFF);
        players[1].clearPatches();

        this.protectedUntil = parent.millis() + 2000;
        
        System.out.println("start");
    }

    public void reset() {
    	System.out.println("reset");
        running = false;
        orblives = 3;
        timeout = false;
        startTime = -1;
        orbcolor = Color.green();

        for(Player player : players) {
          player.configurePatches(Mode.PROX | Mode.ACCEL_XYZ | Mode.ACCEL_INT0 | Mode.ACCEL_INT1);
          player.reset(); //Clears all the particles, scores, patches and gloves
        }

        lastScoreTime = new long[players.length];
        lastScoreTimePauseDiff = new long[players.length];
        coopScore = 0;

        parent.setBackgroundColor(Color.black());
        
            song.pause();
            song.rewind();
            song.setGain(0);
    }

    public void close() {
        song.close();
    }
    

    public void proxEvent(Patch patch) {
        if(!isRunning() || isDone()) return;
        if(!patch.getActive()) return;
        //Handle patch feedback
//        patch.setMode(patch.getZone());
        
        // We will never get prox events from player 1, so we know this is an orb event
        // Three zones: Protected (value > THRESHOLD), Warning (THRESHOLD < value < WARNING, Off (WARNING > value)
        int proxvalue = patch.getProx();
        if (proxvalue > ORB_WARNING) { // Protected
          this.orb.setColor(this.orbcolor);
          this.orb.setActivationMode(this.orb.getActivationMode() | Mode.ACCEL_INT0 | Mode.ACCEL_INT1);
          this.players[1].clearPatches();
          if (proxvalue < ORB_THRESHOLD) {
//            dingding.trigger();
            System.out.println("Warning");
          }
        }
        else {        // Disable orb, enable attackers' patches        
          this.orb.setColor(Color.black());
          this.orb.setActivationMode(this.orb.getActivationMode() & ~(Mode.ACCEL_INT0|Mode.ACCEL_INT1));
          this.players[1].activatePatches();
        }
    }
    
    public void accelXYZEvent(Patch patch) {
      
    }

    // This triggers if "some" movement was detected
    public void accelInterrupt0Event(Patch patch) {
    	if (running && parent.millis() > this.protectedUntil) {
    		System.out.println("Hit!");
            this.protectedUntil = parent.millis() + 2000;
    		
    		if (orblives > 0) {
    			orblives--;
    			switch (orblives) {
    			case 2:
    				this.orbcolor = Color.yellow();
    				break;
    			case 1:
    				this.orbcolor = Color.red();
    				break;
    			case 0:
    				this.orbcolor = Color.black();
    				song.pause();
    				running = false;

    				for(Patch p : this.players[1].getPatches()) {
						p.setColor(Color.white());
						p.setColorDuty(127);
						p.setColorPeriod(HardwareConstants.SLOW_BLINK);
						p.setActive(true);
					}
    				System.out.println("End game");
    				break;
    			}
    			this.orb.setColor(this.orbcolor);
    		}
    		
    		gong.trigger();
    	}
    }

    // This triggers if an impact-like movement was detected
    public void accelInterrupt1Event(Patch patch) {
        dingding.trigger();
        System.out.println("Close one...");
    }

    public void update() {
        for(Player player : players) player.update();
        
        
        long runningtime = parent.millis() - startTime;
        if (runningtime > 60000) {
        	timeout = true;
        	running = false;
        	this.orbcolor = Color.white();
			this.orb.setColorDuty(127);
			this.orb.setColorPeriod(HardwareConstants.SLOW_BLINK);
        }
    }

    public String getName() {
        return name;
    }

    public Player getWinner() {
        Player winner = (orblives > 0) ? players[0] : players[1];
        return winner;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isDone() {
		return (orblives == 0) || timeout;
    }
    
    public void keyPressed(char key, int keyCode) {
        if(!isVisible) return;

        switch(key) {
            case BACKSPACE: {
                reset();
                if(song.position() == 0 || isDone()) parent.changeGameState(GameState.LevelSelect);
                break;
            }

            case ENTER:
            case ' ': {
                if(isDone()) {
                    reset(); //Make sure particles are gone
                    parent.changeGameState(GameState.LevelSelect);
                } else {
                    if(isRunning()) pause();
                    else start();
                }
                break;
            }

            case 'e': { //Force End 
                // song.cue(song.length()-1000);
                startTime = parent.millis()-179000;
                break;
            }
        }
    }

    public void draw() {
        if(!isVisible) return;

        //Particles and Liquid
        if(!PARTICLES_ABOVE) for(int i = 0; i < players.length; i++) players[i].draw();

        //Outlines
        hud.drawInnerBoundary();
        hud.drawOuterBoundary();

        //Score Banners
        if(coop) {
            String score = String.valueOf(coopScore);
            String name = "Coop";

            while(parent.textWidth(score + name) < 240) name += ' ';

            hud.drawBannerCenter(name + score, PlayerConstants.NEUTRAL_COLOR, hud.getAngle());
        } else {
            for(int i = 0; i < players.length; i++) {
                String score = String.valueOf(players[i].score.getScore());
                String name = players[i].getName();

                while(parent.textWidth(score + name) < 240) name += ' ';

                hud.drawBannerSide(name + score, PlayerConstants.PLAYER_COLORS[i], hud.getAngle() - PConstants.HALF_PI + (i * PConstants.PI));
            }
        }

        //Particles and Liquid
        if(PARTICLES_ABOVE) for(int i = 0; i < players.length; i++) players[i].draw();

        if(isDone()) { //Someone won
            Player winner = getWinner();
            String text = winner != null ? winner.getName() + " won!" : "You Tied!";
            Color color = winner != null ? winner.getColor() : PlayerConstants.NEUTRAL_COLOR;
            if(coop) {
                text = "";
                color = PlayerConstants.NEUTRAL_COLOR;
            }
            hud.drawCenterText("", text, color, hud.getAngle());
            hud.drawCenterImage(hud.hudPlayAgain, hud.getAngle());
        } else if(isRunning()) { //Running
            update();
        } else { //Pause
            hud.drawCenterImage(hud.hudPlay, hud.getAngle());
        }
    }

    class VolumeFader implements Runnable {

        Thread thread;

        boolean running, fadeIn;

        public void stop() {
            if(thread != null && thread.isAlive()) {
                running = false;
                thread.interrupt();
                while(thread.isAlive()) Thread.yield();
            }
        }

        public void fadeIn() {
            stop();
            fadeIn = true;
            running = true;
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

        public void fadeOut() {
            stop();
            fadeIn = false;
            running = true;
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

        public void run() {
            if(fadeIn) {
              dingding.trigger();
              try {
                Thread.sleep(dingding.length());
              } catch(Exception e) {

              }
                for(int i = 100;i >= 0;i--) {
                    song.setGain(-(float)i/4);
                    try {
                        Thread.sleep(20);
                    } catch(Exception e) {

                    }
                }
                song.setGain(0);
            } else {
                gong.trigger();

                for(int i = 0;i < 100;i++) {
                    song.setGain(-(float)i/4);
                    try {
                        Thread.sleep(20);
                    } catch(Exception e) {

                    }
                }
                song.setGain(-100);
                song.pause();
            }
        }

    }

}