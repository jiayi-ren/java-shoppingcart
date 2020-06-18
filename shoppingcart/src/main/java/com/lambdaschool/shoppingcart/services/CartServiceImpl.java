package com.lambdaschool.shoppingcart.services;

import com.lambdaschool.shoppingcart.exceptions.ResourceNotFoundException;
import com.lambdaschool.shoppingcart.handlers.HelperFunctions;
import com.lambdaschool.shoppingcart.models.Cart;
import com.lambdaschool.shoppingcart.models.CartItem;
import com.lambdaschool.shoppingcart.models.Product;
import com.lambdaschool.shoppingcart.models.User;
import com.lambdaschool.shoppingcart.repositories.CartRepository;
import com.lambdaschool.shoppingcart.repositories.ProductRepository;
import com.lambdaschool.shoppingcart.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service(value = "cartService")
public class CartServiceImpl
        implements CartService
{
    /**
     * Connects this service to the cart repository
     */
    @Autowired
    private CartRepository cartrepos;

    /**
     * Connects this service the user repository
     */
    @Autowired
    private UserRepository userrepos;

    /**
     * Connects this service to the product repository
     */
    @Autowired
    private ProductRepository productrepos;

    /**
     * Connects this service to the auditing service in order to get current user name
     */
    @Autowired
    private UserAuditing userAuditing;

    @Autowired
    private HelperFunctions helperFunctions;

    @Override
    public List<Cart> findAllByUserId(Long userid)
    {
        User currentUser = userrepos.findById(userid).orElseThrow(() -> new ResourceNotFoundException("User id " + userid + " not found!"));

        if (helperFunctions.isAuthroizedtoMakeChange(currentUser.getUsername())) {
            return cartrepos.findAllByUser_Userid(userid);
        } else {
            throw new ResourceNotFoundException("Not Authorized");
        }

    }

    @Override
    public List<Cart> findAllByAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            User currentUser = userrepos.findByUsername(authentication.getName());
            if (currentUser == null) {
                throw new ResourceNotFoundException("User id " + currentUser.getUserid() + " not found!");
            }
            return cartrepos.findAllByUser_Userid(currentUser.getUserid());
        } else {
            throw new ResourceNotFoundException("No valid token is found!");
        }
    }

    @Override
    public Cart findCartById(long id)
    {
        return cartrepos.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Car id " + id + " not found!"));
    }

    @Transactional
    @Override
    public Cart save(User user,
                     Product product)
    {
        Cart newCart = new Cart();

        User dbuser = userrepos.findById(user.getUserid())
                .orElseThrow(() -> new ResourceNotFoundException("User id " + user.getUserid() + " not found"));
        newCart.setUser(dbuser);

        Product dbproduct = productrepos.findById(product.getProductid())
                .orElseThrow(() -> new ResourceNotFoundException("Product id " + product.getProductid() + " not found"));

        CartItem newCartItem = new CartItem();
        newCartItem.setCart(newCart);
        newCartItem.setProduct(dbproduct);
        newCartItem.setComments("");
        newCartItem.setQuantity(1);
        newCart.getProducts()
                .add(newCartItem);

        return cartrepos.save(newCart);

    }

    @Transactional
    @Override
    public Cart save(Cart cart,
                     Product product)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
                || helperFunctions.isAuthroizedtoMakeChange(findCartById(cart.getCartid()).getUser().getUsername())) {

            Cart updateCart = cartrepos.findById(cart.getCartid())
                    .orElseThrow(() -> new ResourceNotFoundException("Cart Id " + cart.getCartid() + " not found"));
            Product updateProduct = productrepos.findById(product.getProductid())
                    .orElseThrow(() -> new ResourceNotFoundException("Product id " + product.getProductid() + " not found"));

            if (cartrepos.checkCartItems(updateCart.getCartid(), updateProduct.getProductid())
                    .getCount() > 0)
            {
                cartrepos.updateCartItemsQuantity(userAuditing.getCurrentAuditor()
                                                          .get(), updateCart.getCartid(), updateProduct.getProductid(), 1);
            } else
            {
                cartrepos.addCartItems(userAuditing.getCurrentAuditor()
                                               .get(), updateCart.getCartid(), updateProduct.getProductid());
            }

            return cartrepos.save(updateCart);
        } else {
            throw new ResourceNotFoundException("Not Authorized");
        }
    }

    @Transactional
    @Override
    public void delete(Cart cart,
                       Product product)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
                || helperFunctions.isAuthroizedtoMakeChange(findCartById(cart.getCartid()).getUser().getUsername())) {

            Cart updateCart = cartrepos.findById(cart.getCartid())
                    .orElseThrow(() -> new ResourceNotFoundException("Cart Id " + cart.getCartid() + " not found"));
            Product updateProduct = productrepos.findById(product.getProductid())
                    .orElseThrow(() -> new ResourceNotFoundException("Product id " + product.getProductid() + " not found"));

            if (cartrepos.checkCartItems(updateCart.getCartid(), updateProduct.getProductid())
                    .getCount() > 0)
            {
                cartrepos.updateCartItemsQuantity(userAuditing.getCurrentAuditor()
                                                          .get(), updateCart.getCartid(), updateProduct.getProductid(), -1);
                cartrepos.removeCartItemsQuantityZero();
                cartrepos.removeCartWithNoProducts();
            } else
            {
                throw new ResourceNotFoundException("Cart id " + updateCart.getCartid() + " Product id " + updateProduct.getProductid() + " combo not found");
            }
        } else {
            throw new ResourceNotFoundException("Not Authorized");
        }
    }
}
