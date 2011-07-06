/* 
 * The Fascinator - Portal
 * Copyright (C) 2008-2009 University of Southern Queensland
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.googlecode.fascinator.portal;

import java.util.ArrayList;
import java.util.List;

public class Pagination {

    private int page;

    private int totalFound;

    private int startNum;

    private int endNum;

    private int lastPage;

    private List<Page> pages;

    public Pagination(int page, int totalFound, int numPerPage) {

        this.page = page;
        this.totalFound = totalFound;

        lastPage = totalFound / numPerPage;
        if (totalFound % numPerPage > 0) {
            lastPage++;
        }

        int startPage = page - 5;
        if (page < 8) {
            startPage = 1;
        } else if (lastPage - page < 7) {
            startPage = Math.max(lastPage - 10, 1);
        }
        int endPage = Math.min(lastPage, startPage + 10);

        pages = new ArrayList<Page>();
        for (int i = startPage; i < endPage + 1; i++) {
            Page p = new Page(i, page, totalFound, numPerPage);
            pages.add(p);
            if (p.isSelected()) {
                startNum = p.getStart();
                endNum = p.getEnd();
            }
        }
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page < lastPage - 1;
    }

    public int getPage() {
        return page;
    }

    public int getTotalFound() {
        return totalFound;
    }

    public int getLastPage() {
        return lastPage;
    }

    public int getStartNum() {
        return startNum;
    }

    public int getEndNum() {
        return endNum;
    }

    public List<Page> getPages() {
        return pages;
    }
}
