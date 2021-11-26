package com.jongsip.cafe.model

data class Place(
    var placeName: String,             // 장소명, 업체명
    var addressName: String,           // 전체 지번 주소
    var roadAddressName: String,      // 전체 도로명 주소
    var x: String,                      // X 좌표값 혹은 longitude
    var y: String,                     // Y 좌표값 혹은 latitude
    var placeUrl: String,              // 장소 상세페이지 URL
)
