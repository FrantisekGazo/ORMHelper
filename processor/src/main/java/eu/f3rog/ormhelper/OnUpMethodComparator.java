package eu.f3rog.ormhelper;

import javax.lang.model.element.ExecutableElement;

/**
 * Class {@link OnUpMethodComparator}.
 *
 * @author Frantisek Gazo
 * @version 2015-09-26
 */
public class OnUpMethodComparator
        implements java.util.Comparator<ExecutableElement> {

    @Override
    public int compare(ExecutableElement o1, ExecutableElement o2) {
        OnUpgrade onUp1 = o1.getAnnotation(OnUpgrade.class);
        OnUpgrade onUp2 = o2.getAnnotation(OnUpgrade.class);
        if (onUp1.from() == OnUpgrade.UNDEFINED) {
            return onUp1.to() - onUp2.to();
        }
        if (onUp1.from() != onUp2.from()) {
            return onUp1.from() - onUp2.from();
        }
        return onUp1.to() - onUp2.to();
    }

}
