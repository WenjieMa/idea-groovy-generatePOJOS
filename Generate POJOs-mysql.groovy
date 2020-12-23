import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil


typeMapping = [
        (~/(?i)int|decimal\([1-9]\)/)                      : "Integer",
        (~/(?i)decimal\(1[0-8]\)/)                      : "Long",
        (~/(?i)float|double|real|decimal\(\d+\,\d+\)/): "Double",
        (~/(?i)decimal\([2-9]\d\)|decimal\(19\)/): "BigDecimal",
        (~/(?i)datetime|timestamp|date/)       : "Date",
        (~/(?i)/)                         : "String"
]
FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + ".java").withPrintWriter("UTF-8") { out -> generate(table.getName(), out, className, fields) }
}

def generate(tableName, out, className, fields) {

  out.println "import javax.persistence.*;\n" +
          "import java.io.Serializable;\n" +
          "import java.util.Date;\n" +
          "import java.sql.Timestamp;\n" +
          "import java.math.BigDecimal;"
  out.println "@Entity"
  out.println "@Table(name = \"$tableName\")"
  out.println "public class $className implements Serializable {"
  out.println ""
  fields.each() {
    if (it.annos != "") out.println "  ${it.annos}"
    out.println "  private ${it.type} ${it.name}; //${it.comment} "
  }
  out.println ""
  fields.each() {
    if("id".equals(it.name)){
      out.println "  @Id "
    }

    if("Date".equals(it.type)||"DateTime".equals(it.type)){
      out.println "  @Temporal(value =TemporalType.TIMESTAMP) "
    }
    out.println "  @Column(name= \"${it.oriName}\",length= ${it.lentgh})"

    out.println "  public ${it.type} get${it.name.capitalize()}() {"
    out.println "    return ${it.name};"
    out.println "  }"
    out.println ""
    out.println "  public void set${it.name.capitalize()}(${it.type} ${it.name}) {"
    out.println "    this.${it.name} = ${it.name};"
    out.println "  }"
    out.println ""
  }
  out.println "}"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                       name : javaName(col.getName(), false),
                       oriName : col.getName(),
                       type : typeStr,
                       comment: col.getComment()==null?"":col.getComment(),
                       lentgh: (typeStr.equals("Date")||typeStr.equals("Datetime"))?7:col.getDataType().getLength(),
                       annos: ""]]
  }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
          .collect { Case.LOWER.apply(it).capitalize() }
          .join("")
          .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
