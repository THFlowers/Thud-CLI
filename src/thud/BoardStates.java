package thud;

/**
 * Created by Thai Flowers on 5/24/2017.
 *
 * Represents the valid states/pieces allowed in a board square
 * FREE is an empty square
 * STONE is the thud stone, which is immovable in default rules
 * FORBIDDEN is for inaccessible points on the board (the black area in the gui version)
 */
public enum BoardStates {
	DWARF, TROLL, STONE, FREE, FORBIDDEN
}
