package com.testerhome.appcrawler

import org.apache.commons.lang3.StringEscapeUtils
import javassist.{ClassPool, CtConstructor}

import scala.util.{Failure, Success, Try}

/**
  * Created by seveniruby on 2017/4/15.
  */
object SuiteToClass extends CommonLog {

  var index=0

  //todo: 使用白名单
  def format(name:String): String ={
    name
      .replaceAllLiterally("\\", "\\\\")
      .replaceAllLiterally("\"", "\\\"")
      .replaceAllLiterally("#", "")
      .replaceAllLiterally("&", "")
      .replaceAllLiterally("-", ".")
      .replaceAll("[#;/\\:]", "")
  }
  /**
    * 生成用例对应的class文件，用于调用scalatest执行
    *
    */
  def genTestCaseClass(className: String, superClassName: String, fields: Map[String, Any], directory: String): Unit = {
    val pool = ClassPool.getDefault
    val classNameFormat = format(className)
    log.trace(s"classNameFormat=${classNameFormat}")
    val clazz=pool.getOrNull(classNameFormat)
    if(clazz!=null){
      if(clazz.isFrozen){
        clazz.defrost()
      }
    }
    Try(pool.makeClass(classNameFormat)) match {
      case Success(classNew) => {
        classNew.setSuperclass(pool.get(superClassName))
        val init = new CtConstructor(null, classNew)
        val body = fields.map(field => {
          //todo: 字段与类名分开
          //todo: 使用数据驱动解决
          s"${field._1}_$$eq(${'"' + format(field._2.toString) + '"'}); "
        }).mkString("\n")
        log.trace(body)
        init.setBody(s"{ ${body}\naddTestCase(); }")
        classNew.addConstructor(init)
        classNew.writeFile(directory)
        log.debug(s"write to ${directory}")

      }
      case Failure(e) => {
        log.error(s"makeClass error with ${className}")
        log.error(e.getMessage)
        log.error(e.getStackTrace.mkString("\n"))
      }
    }
  }

}

