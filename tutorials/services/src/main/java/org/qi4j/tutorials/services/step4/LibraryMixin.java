/*
 * Copyright 2009 Niclas Hedhman.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.tutorials.services.step4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import org.qi4j.api.configuration.Configuration;
import org.qi4j.api.injection.scope.Structure;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.value.ValueBuilder;
import org.qi4j.api.value.ValueBuilderFactory;

public class LibraryMixin
    implements Library
{
    private HashMap<String, ArrayList<Book>> books;

    public LibraryMixin( @This Configuration<LibraryConfiguration> config, @Structure ValueBuilderFactory factory )
    {
        books = new HashMap<String, ArrayList<Book>>();
        String titles = config.get().titles().get();
        String authors = config.get().authors().get();
        int copies = config.get().copies().get();
        StringTokenizer titlesSt = new StringTokenizer( titles, ",", false );
        StringTokenizer authorSt = new StringTokenizer( authors, ",", false );
        while( titlesSt.hasMoreTokens() )
        {
            String title = titlesSt.nextToken();
            String author = authorSt.nextToken();
            createBook( factory, author, title, copies );
        }
    }

    @Override
    public Book borrowBook( String author, String title )
    {
        String key = constructKey( author, title );
        ArrayList<Book> copies = books.get( key );
        if( copies == null )
        {
            System.out.println( "Book not available." );
            return null; // Indicate that book is not available.
        }
        Book book = copies.remove( 0 );
        if( book == null )
        {
            System.out.println( "Book not available." );
            return null; // Indicate that book is not available.
        }
        System.out.println( "Book borrowed: " + book.title().get() + " by " + book.author().get() );
        return book;
    }

    @Override
    public void returnBook( Book book )
    {
        System.out.println( "Book returned: " + book.title().get() + " by " + book.author().get() );
        String key = constructKey( book.author().get(), book.title().get() );
        ArrayList<Book> copies = books.get( key );
        if( copies == null )
        {
            throw new IllegalStateException( "Book " + book + " was not borrowed here." );
        }
        copies.add( book );
    }

    private void createBook( ValueBuilderFactory factory, String author, String title, int copies )
    {
        ArrayList<Book> bookCopies = new ArrayList<Book>();
        String key = constructKey( author, title );
        books.put( key, bookCopies );

        for( int i = 0; i < copies; i++ )
        {
            ValueBuilder<Book> builder = factory.newValueBuilder( Book.class );
            Book prototype = builder.prototype();
            prototype.author().set( author );
            prototype.title().set( title );

            Book book = builder.newInstance();
            System.out.println( "Book created: " + book );
            bookCopies.add( book );
        }
    }

    private String constructKey( String author, String title )
    {
        return author + "::" + title;
    }
}
