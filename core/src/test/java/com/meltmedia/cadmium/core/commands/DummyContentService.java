package com.meltmedia.cadmium.core.commands;

import com.meltmedia.cadmium.core.ContentService;

public class DummyContentService implements ContentService {
  
  public boolean switched = false;

  @Override
  public void switchContent() {
    switched = true;
  }

}