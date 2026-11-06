package com.bookcanvas.domain.book.dto;

/**
 * 기존 책을 예스24 책 정보로 연결한 결과
 *
 * @param linked    예스24 에서 찾아 연결한 책 수
 * @param notFound  예스24 에서 같은 책을 찾지 못한 책 수 (다시 시도하지 않는다)
 * @param remaining 이번에 처리하지 못하고 남은 책 수 (한 번에 처리하는 수를 넘었거나 호출 한도에 걸린 경우)
 * @param stopped   예스24 호출이 막혀(키 오류·호출 한도·장애) 중간에 멈췄는지
 */
public record Yes24LinkResultDto(int linked, int notFound, int remaining, boolean stopped) {
}
