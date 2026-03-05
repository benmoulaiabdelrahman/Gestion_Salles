package com.gestion.salles.views.Admin;

import com.gestion.salles.views.shared.management.TablePrintableBase;

import javax.swing.*;
import java.util.List;

public class BlocTablePrintable extends TablePrintableBase {

    public BlocTablePrintable(JTable tableToPrint, String universityTitle, String dynamicFilterInfo, List<String> visibleColumns) {
        super(tableToPrint, universityTitle, dynamicFilterInfo, visibleColumns);
    }
}
