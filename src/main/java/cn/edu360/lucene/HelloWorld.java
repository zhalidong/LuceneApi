package cn.edu360.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by zx on 2017/9/12.
 * <p>
 * Lucene Index ToolBox : https://github.com/DmitryKey/luke/releases
 * <p>
 * https://segmentfault.com/a/1190000010367206
 */
public class HelloWorld {


    /**
     * 往用lucene写入数据
     * @throws IOException
     */
    @Test
    public void testCreate() throws IOException {
		//自定义类Article
        Article article = new Article();
        article.setId(10011L);
        article.setAuthor("段子王");
        article.setTitle("最会讲段子的老师");
        article.setContent("学大数据，到小牛学堂 ！");
        article.setUrl("http://www.edu360.cn/a10011");
		//本地目录
        String indexPath = "D:/Users/zx/Documents/dev/lucene/index";
		//打开目录
        FSDirectory fsDirectory = FSDirectory.open(Paths.get(indexPath));
        //创建一个标准分词器，一个字分一次
        //Analyzer analyzer = new StandardAnalyzer();
        //智能分词器 中文分词器
        Analyzer analyzer = new IKAnalyzer(true);
        //写入索引的配置，设置了分词器
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        //指定了写入数据目录和配置
        IndexWriter indexWriter = new IndexWriter(fsDirectory, indexWriterConfig);
        //创建一个文档对象 并转换成lucene识别的类型
        Document document = article.toDocument();
        //通过IndexWriter写入
        indexWriter.addDocument(document);
        indexWriter.close();
    }

    @Test
    public void testSearch() throws IOException, ParseException {

        String indexPath = "D:/Users/zx/Documents/dev/lucene/index";
        //Analyzer analyzer = new StandardAnalyzer();
        Analyzer analyzer = new IKAnalyzer(true);
        DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        //索引查询器
        IndexSearcher indexSearcher = new IndexSearcher(directoryReader);

        String queryStr = "河蟹";
        //创建一个查询条件解析器
        QueryParser parser = new QueryParser("content", analyzer);
        //对查询条件进行解析
        Query query = parser.parse(queryStr);
        //把"老赵"当一个词来查 TermQuery将查询条件当成是一个固定词
        //Query query = new TermQuery(new Term("author",queryStr));
         //TermQuery将查询条件当成是一个固定的词
        //Query query = new TermQuery(new Term("url", "http://www.edu360.cn/a10010"));
        //在【索引】中进行查找  前10个
        TopDocs topDocs = indexSearcher.search(query, 10);

        //获取到查找到的文档ID和得分
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            //从索引中查询到文档的ID，
            int doc = scoreDoc.doc;
            //在根据ID到文档中查找文档内容
            Document document = indexSearcher.doc(doc);
            //将文档转换成对应的实体类
            Article article = Article.parseArticle(document);
            System.out.println(article);
        }

        directoryReader.close();
    }

    @Test
    public void testDelete() throws IOException, ParseException {

        String indexPath = "D:/Users/zx/Documents/dev/lucene/index";
        Analyzer analyzer = new IKAnalyzer(true);
        FSDirectory fsDirectory = FSDirectory.open(Paths.get(indexPath));
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(fsDirectory, indexWriterConfig);

        //Term词条查找，内容必须完全匹配，不分词(Term)
        //indexWriter.deleteDocuments(new Term("content", "学好"));
        //根据分词器来查
        //QueryParser parser = new QueryParser("title", analyzer);
        //Query query = parser.parse("大数据老师");

        //LongPoint是建立索引的
        //Query query = LongPoint.newRangeQuery("id", 99L, 120L);
        Query query = LongPoint.newExactQuery("id", 105L);

        indexWriter.deleteDocuments(query);

        indexWriter.commit();
        indexWriter.close();
    }

    /**
     * lucene的update比较特殊，update的代价太高，先删除，然后在插入
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testUpdate() throws IOException, ParseException {

        String indexPath = "/Users/zx/Documents/dev/lucene/index";
        StandardAnalyzer analyzer = new StandardAnalyzer();
        FSDirectory fsDirectory = FSDirectory.open(Paths.get(indexPath));
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(fsDirectory, indexWriterConfig);


        Article article = new Article();
        article.setId(106L);
        article.setAuthor("老王");
        article.setTitle("学好大数据，要找赵老师");
        article.setContent("迎娶白富美，走上人生巅峰！！！");
        article.setUrl("http://www.edu360.cn/a111");
        Document document = article.toDocument();

        indexWriter.updateDocument(new Term("author", "老王"), document);

        indexWriter.commit();
        indexWriter.close();
    }

    /**
     * 可以从多个字段中查找
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testMultiField() throws IOException, ParseException {

        String indexPath = "/Users/zx/Documents/dev/lucene/index";
        Analyzer analyzer = new IKAnalyzer(true);
        DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher indexSearcher = new IndexSearcher(directoryReader);

        String[] fields = {"title", "content"};
        //多字段的查询转换器
        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fields, analyzer);
        Query query = queryParser.parse("老师");

        TopDocs topDocs = indexSearcher.search(query, 10);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            int doc = scoreDoc.doc;
            Document document = indexSearcher.doc(doc);
            Article article = Article.parseArticle(document);
            System.out.println(article);
        }

        directoryReader.close();
    }

    /**
     * 查找全部的数据
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testMatchAll() throws IOException, ParseException {

        String indexPath = "D:/Users/zx/Documents/dev/lucene/index";
        DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher indexSearcher = new IndexSearcher(directoryReader);

        Query query = new MatchAllDocsQuery();

        TopDocs topDocs = indexSearcher.search(query, 10);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            int doc = scoreDoc.doc;
            Document document = indexSearcher.doc(doc);
            Article article = Article.parseArticle(document);
            System.out.println(article);
        }

        directoryReader.close();
    }

    /**
     * 布尔查询，可以组合多个查询条件
     * @throws Exception
     */
    @Test
    public void testBooleanQuery() throws Exception {
        String indexPath = "/Users/zx/Documents/dev/lucene/index";
        DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher indexSearcher = new IndexSearcher(directoryReader);

        Query query1 = new TermQuery(new Term("title", "老师"));
        Query query2 = new TermQuery(new Term("content", "丁"));
        BooleanClause bc1 = new BooleanClause(query1, BooleanClause.Occur.MUST);
        BooleanClause bc2 = new BooleanClause(query2, BooleanClause.Occur.MUST_NOT);
        BooleanQuery boolQuery = new BooleanQuery.Builder().add(bc1).add(bc2).build();
        System.out.println(boolQuery);

        TopDocs topDocs = indexSearcher.search(boolQuery, 10);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            int doc = scoreDoc.doc;
            Document document = indexSearcher.doc(doc);
            Article article = Article.parseArticle(document);
            System.out.println(article);
        }

        directoryReader.close();
    }

    @Test
    public void testQueryParser() throws Exception {
        String indexPath = "/Users/zx/Documents/dev/lucene/index";
		
        DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher indexSearcher = new IndexSearcher(directoryReader);

        //创建一个QueryParser对象。参数1：默认搜索域 参数2：分析器对象。
        QueryParser queryParser = new QueryParser("title", new IKAnalyzer(true));

        //Query query = queryParser.parse("数据");
		Query query = queryParser.parse("title:学好 OR title:学习");
        System.out.println(query);

        TopDocs topDocs = indexSearcher.search(query, 10);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            int doc = scoreDoc.doc;
            Document document = indexSearcher.doc(doc);
            Article article = Article.parseArticle(document);
            System.out.println(article);
        }

        directoryReader.close();
    }


    @Test
    public void testRangeQuery() throws Exception {
        String indexPath = "/Users/zx/Documents/dev/lucene/index";
        DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher indexSearcher = new IndexSearcher(directoryReader);


        Query query = LongPoint.newRangeQuery("id", 107L, 108L);

        System.out.println(query);

        TopDocs topDocs = indexSearcher.search(query, 10);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            int doc = scoreDoc.doc;
            Document document = indexSearcher.doc(doc);
            Article article = Article.parseArticle(document);
            System.out.println(article);
        }

        directoryReader.close();
    }
}
