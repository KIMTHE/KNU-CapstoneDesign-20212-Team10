package com.jongsip.cafe.util

object WifiLoginUtils {
    private val idDiv = arrayListOf("ID", "id", "이름", "아이디","wifi","WIFI","와이파이","WiFi")
    private val pwDiv = arrayListOf("PW", "pw", "password", "PASSWORD", "비밀번호", "비번")
    var idCase = arrayListOf<String>()
    var pwCase = arrayListOf<String>()

    //message 에서 div 의 모든 경우의수로 id, pw 추출
    fun extract(message: String) {
        idCase = arrayListOf<String>()
        pwCase = arrayListOf<String>()
        val token = message.split("\n")
        
        //ID,PW
        for (line in token) {
            for (div in pwDiv) {
                if (!line.contains(div)) continue
                manufacture(pwCase, line, div)
            }

            for (div in idDiv) {
                if (!line.contains(div)) continue
                manufacture(idCase, line, div)
            }
        }

        //message 에 id의 div 가 포함되지 않을 경우
        if (idCase.size == 0) {
            for (line in token) {
                if(line == " ") continue
                var inPwCase = false

                for (div in pwDiv) {
                    if (line.contains(div)) {
                        inPwCase = true
                        break
                    }
                }
                if (!inPwCase) manufacture(idCase, line, null)
            }
        }
    }

    //id,pw 문자열 가공
    private fun manufacture(cases: ArrayList<String>, line: String, div: String?) {
        var case = line

        if (div != null) case = case.split(div)[1]

        case = case.replace(":", "")

        //앞의 공백 제거
        for (i in case.indices) {
            if (case[i].toString() != " ") {
                case = case.substring(i)
                break
            }
        }

        //뒤의 공백 제거
        for (i in case.length - 1 downTo 0) {
            if (case[i].toString() != " ") {
                case = case.substring(0, i+1)
                break
            }
        }

        cases.add(case)
    }


}