package org.openxdata.markup.serializer

import groovy.json.JsonOutput
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import org.openxdata.markup.*

/**
 * Created with IntelliJ IDEA.
 * User: kay
 * Date: 2/4/13
 * Time: 4:49 PM
 * To change this template use File | Settings | File Templates.
 */
class XFormSerializer {


    boolean numberQuestions = false
    boolean numberBindings = false
    boolean putExtraAttributesInComments = false
    boolean generateView = true

    Map<Form, String> xforms = [:]
    def studyXML

    public String toStudyXml(Study study) {
        def printWriter = new StringWriter();
        def xml = new MarkupBuilder(printWriter)
        xml.setDoubleQuotes(true)

        println "========== Converting study [${study?.name}] to XML"
        xml.study(name: study.name) {

            study.forms.each { form ->
                xml.form(name: form.name) {
                    xml.version(name: 'v1') {
                        xml.xform(toXForm(form))
                        if (generateView)
                            xml.layout(toLayout(form))
                    }


                }
            }
        }
        println "========== Done converting study [${study?.name}] ${new Date()}"
        studyXML = printWriter.toString()
        return studyXML
    }

    public Map<String, String> getFormImports() {

        def imports = [:]

        def studyNode = new XmlParser().parseText(studyXML)

        studyNode.form.each { Node node ->
            def formName = node.'@name'
            imports[formName + ".form"] = XmlUtil.serialize(node)

            def versionName = node.version[0].'@name'
            imports["$formName-${versionName}.version"] = XmlUtil.serialize(node.version[0])
        }

        return imports
    }

    private String toLayout(Form form) {
        def laySer = new LayoutSerializer(numberText: numberQuestions, numberBindings: numberBindings)
        def layout = laySer.generateLayout(form)
        layout
    }

    public String toXForm(Form form) {
        def printWriter = new StringWriter();
        def xml = new MarkupBuilder(printWriter)
        xml.doubleQuotes = true
        checkBindLength(form.binding)
        xml.xforms {
            xml.model {
                xml.instance(id: form.binding) {
                    xml."$form.binding"(id: form.dbId ?: 0, name: form.name, formKey: form.binding) {

                        form.questions.each { question ->
                            def bind = binding(question)
                            checkBindLength(bind)
                            xml."$bind"(question.value) {//todo Improve and use recursion when you have time
                                if (question instanceof RepeatQuestion) {
                                    RepeatQuestion qn = question
                                    qn.questions.each { qnInRpt ->
                                        checkBindLength(binding(qnInRpt))
                                        xml."${binding(qnInRpt)}"(qnInRpt.value)
                                    }
                                }
                            }
                        }
                    }
                }
                buildDynamicModel(xml, form)
                form.questions.each {
                    if (it instanceof RepeatQuestion) {    //We do not support recursion. repeats with in repeats
                        addBindNode(xml, it)
                        it.questions.each {
                            addBindNode(xml, it)
                        }
                    } else
                        addBindNode(xml, it)
                }
            }

            form.pages.eachWithIndex { page, idx ->
                xml.group(id: idx + 1) {
                    xml.label(page.name)
                    buildQuestionsLayout(page, xml)

                }
            }
        }

        def xFormXml = printWriter.toString()
        xforms[form] = xFormXml
        return xFormXml
    }

    private String binding(IQuestion question) {
        if (numberBindings)
            return question.getBinding(numberBindings)
        return question.binding

    }

    private String getAbsoluteBindingXPath(String xPath, IQuestion question) {
        if (numberBindings)
            return Form.getIndexedAbsoluteBindingXPath(xPath, question)
        return Form.getAbsoluteBindingXPath(xPath, question)
    }

    private String absoluteBinding(IQuestion question) {
        if (numberBindings)
            return question.indexedAbsoluteBinding
        return question.absoluteBinding
    }

    private static void checkBindLength(String bind) {
        if (bind.length() > 63)
            System.err.println "Binding: [$bind] is too long"
    }

    private void addBindNode(MarkupBuilder xml, IQuestion question) {

        def type = getQuestionType(question)

        def map = [id: binding(question), nodeset: absoluteBinding(question)] + question.bindAttributes

        if (type.type) map['type'] = type.type

        if (type.format) map['format'] = type.format

        if (question.isRequired()) map['required'] = "true()"

        if (question.isReadOnly()) map['locked'] = "true()"

        if (!question.isVisible()) map['visible'] = "false()"

        if (question.skipLogic) {
            def xpath = getAbsoluteBindingXPath(question.skipLogic, question)
            map['relevant'] = xpath
            map['action'] = question.skipAction
        }

        if (question.validationLogic) {
            def xpath = getAbsoluteBindingXPath(question.validationLogic, question)
            map['constraint'] = xpath
            map['message'] = question.message
        }

        if (question.calculation) {
            def xpath = getAbsoluteBindingXPath(question.calculation, question)
            map['calculate'] = xpath
        }

        xml.bind(map)
    }

    private void buildQuestionsLayout(HasQuestions page, MarkupBuilder xml) {
        page.questions.each { question ->
            def questionClass = question.class

            switch (questionClass) {
                case RepeatQuestion.class:
                    buildRepeatLayout(xml, question, page)
                    break
                case MultiSelectQuestion.class:
                    buildSelectionLayout(xml, question)
                    break
                case SingleSelectQuestion.class:
                    buildSelectionLayout(xml, question)
                    break
                case DynamicQuestion.class:
                    buildDynamicLayout(xml, question, page)
                    break
                default:
                    buildQuestionLayout(xml, question)
                    break
            }
        }
    }


    void buildDynamicModel(MarkupBuilder xml, Form form) {
        def completeBinds = []
        form.allQuestions.each { question ->
            if (!(question instanceof DynamicQuestion))
                return

            if (completeBinds.contains(question.dynamicInstanceId))
                return

            xml.instance(id: question.dynamicInstanceId) {
                completeBinds << question.dynamicInstanceId
                xml.dynamiclist {

                    List<DynamicOption> options = question.options
                    options.each { option ->
                        xml.item(id: option.bind, parent: option.parentBinding) {
                            xml.label(option.child)
                            xml.value(option.bind)
                        }
                    }
                }
            }
        }
    }


    void buildQuestionLayout(MarkupBuilder xml, IQuestion question) {
        def qnType = getQuestionType(question)
        if (qnType.type == 'xsd:base64Binary') {
            def map = [bind: binding(question), mediatype: "${qnType.format}/*"] + question.layoutAttributes
            xml.upload(map) {
                buildQuestionLabelAndHint(xml, question)
            }
        } else {
            def map = [bind: binding(question)] + question.layoutAttributes
            xml.input(map) {
                buildQuestionLabelAndHint(xml, question)
            }
        }
    }

    void buildQuestionLabelAndHint(MarkupBuilder xml, IQuestion question) {

        def label = question.getText(numberQuestions)
        xml.label(label)
        def comment = question.comment

        if (putExtraAttributesInComments) {
            def commentMap = [:]

            if (question.bindAttributes) {
                commentMap['bind'] = question.bindAttributes
            }

            if (question.layoutAttributes) {
                commentMap['layout'] = question.layoutAttributes
            }

            if (commentMap) {

                if (comment) commentMap['comment'] = comment

                comment = "json:${JsonOutput.toJson(commentMap)}"
            }

        }

        if (comment) {
            xml.hint(comment)
        }
    }

    void buildDynamicLayout(MarkupBuilder xml, DynamicQuestion question, HasQuestions page) {

        def map = [bind: binding(question)] + question.layoutAttributes
        xml.select1(map) {
            //"instance('district')/item[@parent=instance('brent_study_fsdfsd_v1')/country]
            buildQuestionLabelAndHint(xml, question)
            xml.itemset(nodeset: "instance('$question.dynamicInstanceId')/item[@parent=instance('$page.parentForm.binding')/${getDynamicParentQnId(question)}]") {
                xml.label(ref: 'label')
                xml.value(ref: 'value')
            }

        }
    }

    private String getDynamicParentQnId(DynamicQuestion question) {
        if (numberBindings)
            return question.indexedParentQuestionId
        return question.parentQuestionId
    }

    void buildSelectionLayout(MarkupBuilder xml, ISelectionQuestion question) {

        def selectRef = question instanceof SingleSelectQuestion ? '1' : ''
        def map = [bind: binding(question)] + question.layoutAttributes
        xml."select$selectRef"(map) {
            buildQuestionLabelAndHint(xml, question)
            question.options.each { option ->
                xml.item(id: option.bind) {
                    xml.label(option.text)
                    xml.value(option.bind)
                }
            }

        }
    }


    void buildRepeatLayout(MarkupBuilder xml, RepeatQuestion question, HasQuestions page) {

        xml.group(id: binding(question)) {
            buildQuestionLabelAndHint(xml, question)


            def map = [bind: binding(question)] + question.layoutAttributes
            xml.repeat(map) {
                buildQuestionsLayout(question, xml)

            }
        }
    }

    static Map getQuestionType(IQuestion question) {
        switch (question.type) {
            case 'video':
                return [type: 'xsd:base64Binary', format: 'video']
            case 'picture':
                return [type: 'xsd:base64Binary', format: 'image']
            case 'audio':
                return [type: 'xsd:base64Binary', format: 'audio']
            case 'number':
                return [type: 'xsd:int']
            case 'gps':
                return [type: 'xsd:string', format: 'gps']
            case 'repeat':
                return [:]
            case 'longtext':
                return [type: 'xsd:string']
            default:
                return [type: "xsd:$question.type"]

        }
    }

}
