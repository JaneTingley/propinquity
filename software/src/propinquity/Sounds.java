package propinquity;

import processing.core.PApplet;
import ddf.minim.*;

/**
 * Handles sound content initialization and usage.
 * 
 * @author Stephane Beniak
 */
public class Sounds {

	public static final String SONG_FOLDER = "songs/";
	public static final int BUFFER_SIZE = 2048;

	Minim minim;

	AudioPlayer bubbleLow, bubbleHigh;
	AudioPlayer complete;
	AudioPlayer positive;
	AudioPlayer negativeCoop;
	AudioPlayer[] negativePlayer;

	/**
	 * Setup the Minim audio manager.
	 * 
	 * @param parent
	 */
	public Sounds(PApplet parent) {
		minim = new Minim(parent);

		bubbleLow = minim.loadFile("sounds/bubble350Hz.mp3", BUFFER_SIZE);
		bubbleHigh = minim.loadFile("sounds/bubble600Hz.mp3", BUFFER_SIZE);
		
		complete = minim.loadFile("sounds/comp.mp3", BUFFER_SIZE);
		complete.setGain(5);

		positive = minim.loadFile("sounds/pos.mp3", BUFFER_SIZE);

		negativeCoop = minim.loadFile("sounds/neg.mp3", BUFFER_SIZE);

		negativePlayer = new AudioPlayer[2];
		negativePlayer[0] = minim.loadFile("sounds/neg1.mp3", BUFFER_SIZE);
		negativePlayer[1] = minim.loadFile("sounds/neg2.mp3", BUFFER_SIZE);
	}

	/**
	 * Load the song for the current level.
	 */
	public AudioPlayer loadSong(String file) {
		return minim.loadFile(SONG_FOLDER + file, BUFFER_SIZE);
	}

	public AudioPlayer getComplete() {
		return complete;
	}

	public AudioPlayer getPositive() {
		return positive;
	}

	public AudioPlayer getNegativeCoop() {
		return negativeCoop;
	}

	public AudioPlayer getNegativePlayer(int player) {
		return negativePlayer[PApplet.constrain(player, 0, negativePlayer.length-1)];
	}

}
