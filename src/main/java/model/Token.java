package model;

import enums.TokenType;

public record Token(TokenType type, String lexeme, Object literal, int line) {
}
