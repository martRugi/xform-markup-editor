package org.openxdata.markup

/**
 * Created with IntelliJ IDEA.
 * User: kay
 * Date: 1/29/13
 * Time: 9:30 PM
 * To change this template use File | Settings | File Templates.
 */
interface IQuestion {

    void setReadOnly(boolean readOnly)

    boolean isReadOnly()

    void setRequired(boolean required)

    boolean isRequired()

    String getText()

    void setText(String text)

    String getBinding()

    void setBinding(String binding)

    void setComment(String comment)

    String getAbsoluteBinding()

    String getIndexedAbsoluteBinding()

    String getIndexedRelativeBinding()

    String getRelativeBinding()

    String getIndexedBinding()

    String getAbsoluteBinding(boolean indexed, boolean relative)

    String getComment()

    String getType()

    void setType(String type)

    boolean isVisible()

    void setVisible(boolean visible)

    void setParent(HasQuestions hasQuestions)

    HasQuestions getParent()

    Form getParentForm()

    String getSkipLogic()

    String getSkipAction()

    void setSkipLogic(String skipLogic)

    void setSkipAction(String skipAction)

    String getValidationLogic()

    void setValidationLogic(String validationLogic)

    String getMessage()

    void setMessage(String message)

    void setCalculation(String calculation)

    String getCalculation()

    def getQuestionIdx()

    def getContextIdx()

    String getBinding(boolean numbered)

    String getText(boolean number)

    int getLine()

    void setLine(int i)

    void setHasAbsoluteId(boolean hasAbsoluteId)

    boolean getHasAbsoluteId()

    def getValue()

    void setValue(def value)

    Map getBindAttributes()

    Map getLayoutAttributes()

}
