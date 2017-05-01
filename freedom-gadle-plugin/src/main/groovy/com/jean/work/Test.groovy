package com.jean.work

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

/**
 * Created by rantianhua on 17/4/29.
 */
def list1 = ["1", "2", "3", "4"]
for (item in list1) {
    println(item)
    if (item == "3") {
        break
    }
}
if ("1" in list1) {
    println(true)
}
def list2 = list1 - ["1", "2"]

println(list1)
println(list2)

class Fm {
    String f
    String m
}

def fms = [new Fm(f: "f1", m: "m1"), new Fm(f: "f2", m: "m2"),new Fm(f: "f3", m: "m3")]

def map = [:]
map.put("f", fms)

//def jsonBuilder = new JsonBuilder()
//jsonBuilder.app {
//    state "add"
//    content map.get("f") as List ?: [], { item ->
//        f item.f
//        m item.m
//    }
//}
//jsonBuilder.app map.get("f") as List ?: [], { item ->
//    f item.f
//    m item.m
//}
//String app = "fff"
//jsonBuilder {
//    "${app}" list1
//}
//jsonBuilder{
//    dd list2
//}


//def parse = new JsonSlurper().parseText(jsonBuilder.toString())
//def parseFms = parse.app
//println(parseFms)

//println(jsonBuilder.toPrettyString())
//
//JsonBuilder js = new JsonBuilder()
//def result = js.a {
//    f "f"
//}
//
//result.put("m", {
//    m "m"
//})
//
//println(new JsonBuilder(result).toPrettyString())

File file = new File("/Users/rantianhua/bs/Freedom")
file.eachFile {
    println(it.name)
}