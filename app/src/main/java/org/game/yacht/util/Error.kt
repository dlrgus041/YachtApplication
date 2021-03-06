package org.game.yacht.util

enum class Error(val ex: String) {
    CONNECTION_REFUSED("서버와 연결이 실패했습니다."),
    NETWORK_BROKEN("연결이 끊겼습니다."),
    OPPONENT_LEFT("상대가 나갔습니다."),
    UNDEFINED_ERROR("알 수 없는 오류가 발생했습니다.")
}