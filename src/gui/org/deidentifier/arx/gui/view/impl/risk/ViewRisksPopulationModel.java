/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.gui.view.impl.risk;


import java.text.DecimalFormat;

import org.deidentifier.arx.ARXPopulationModel;
import org.deidentifier.arx.ARXPopulationModel.Region;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.gui.Controller;
import org.deidentifier.arx.gui.model.Model;
import org.deidentifier.arx.gui.model.ModelEvent;
import org.deidentifier.arx.gui.model.ModelEvent.ModelPart;
import org.deidentifier.arx.gui.view.SWTUtil;
import org.deidentifier.arx.gui.view.def.IView;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

/**
 * This view displays the population settings
 * 
 * @author Fabian Prasser
 */
public class ViewRisksPopulationModel implements IView {

    /** Controller */
    private final Controller controller;

    /** View */
    private final Composite  root;
    /** View */
    private List             list;
    /** View */
    private Text             text;
    /** View */
    private Text             text2;
    /** View */
    private DecimalFormat    format = new DecimalFormat("0.########################################");

    /** Model */
    private Model            model;


    /**
     * Creates a new instance.
     * 
     * @param parent
     * @param controller
     */
    public ViewRisksPopulationModel(final Composite parent,
                                    final Controller controller) {

        controller.addListener(ModelPart.INPUT, this);
        controller.addListener(ModelPart.POPULATION_MODEL, this);
        controller.addListener(ModelPart.MODEL, this);
        controller.addListener(ModelPart.OUTPUT, this);
        controller.addListener(ModelPart.RESULT, this);
        this.controller = controller;

        // Create group
        root = parent;
        root.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
        create(root);
        reset();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.deidentifier.arx.gui.view.def.IView#dispose()
     */
    @Override
    public void dispose() {
        controller.removeListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.deidentifier.arx.gui.view.def.IView#reset()
     */
    @Override
    public void reset() {
        text.setText("");
        text2.setText("");
        SWTUtil.disable(root);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.deidentifier.arx.gui.view.def.IView#update(org.deidentifier.arx.gui
     * .model.ModelEvent)
     */
    @Override
    public void update(final ModelEvent event) {
        if (event.part == ModelPart.MODEL) {
           this.model = (Model) event.data;
           update();
        } else if (event.part == ModelPart.INPUT ||
                   event.part == ModelPart.POPULATION_MODEL ||
                   event.part == ModelPart.OUTPUT ||
                   event.part == ModelPart.RESULT) {
           update();
        }
    }

    /**
     * Creates the required controls.
     * 
     * @param parent
     */
    private void create(final Composite parent) {

        Label lbl1 = new Label(parent, SWT.NONE);
        lbl1.setText("Region:");
        lbl1.setLayoutData(GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.TOP).create());
        
        list = new List(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        for (Region region : Region.values()) {
            list.add(region.getName());
        }
        list.setLayoutData(new GridData(GridData.FILL_BOTH));
        list.setEnabled(false);
        
        Label lbl2 = new Label(parent, SWT.NONE);
        lbl2.setText("Sample fraction:");
        
        text = new Text(parent, SWT.BORDER | SWT.SINGLE);
        text.setText("0");
        text.setLayoutData(SWTUtil.createFillHorizontallyGridData());
        text.setEditable(false);
        
        Label lbl3 = new Label(parent, SWT.NONE);
        lbl3.setText("Population size:");
        
        text2 = new Text(parent, SWT.BORDER | SWT.SINGLE);
        text2.setText("0");
        text2.setLayoutData(SWTUtil.createFillHorizontallyGridData());
        text2.setEditable(false);
        
        list.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent arg0) {
                
                if (model == null || model.getInputConfig() == null ||
                    model.getInputConfig().getInput() == null) {
                    return;
                }
                
                ARXPopulationModel popmodel = model.getInputPopulationModel();
                if (model.getOutputPopulationModel() != null) {
                    popmodel = model.getOutputPopulationModel();
                }

                for (int i=0; i<list.getItemCount(); i++) {
                    if (list.getItem(i).equals(popmodel.getRegion().getName())) {
                        list.select(i);
                        break;
                    }
                }
            }
        });
    }

    /**
     * Updates the view.
     * 
     * @param node
     */
    private void update() {

        // Check
        if (model == null || model.getInputConfig() == null ||
            model.getInputConfig().getInput() == null) { 
            return; 
        }
        
        root.setRedraw(false);
        SWTUtil.enable(root);
        
        ARXPopulationModel popmodel = model.getInputPopulationModel();
        if (model.getOutputPopulationModel() != null) {
            popmodel = model.getOutputPopulationModel();
        }
        
        for (int i=0; i<list.getItemCount(); i++) {
            if (list.getItem(i).equals(popmodel.getRegion().getName())) {
                list.select(i);
                break;
            }
        }
        
        DataHandle handle = model.getInputConfig().getInput().getHandle();
        text.setText(format.format(popmodel.getSampleFraction(handle)));
        text.setEnabled(true);
        text2.setText(format.format(popmodel.getPopulationSize(handle)));
        text2.setEnabled(true);
        root.setRedraw(true);
    }
}